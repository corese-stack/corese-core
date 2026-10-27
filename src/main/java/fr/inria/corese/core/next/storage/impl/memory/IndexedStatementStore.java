package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.exception.ErrorCode;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.storage.api.lifecycle.StorageLifecycle;
import fr.inria.corese.core.next.storage.api.model.PredicateStatistics;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import fr.inria.corese.core.next.storage.api.model.StorageStatistics;
import fr.inria.corese.core.next.storage.api.operations.MetadataOperations;
import fr.inria.corese.core.next.storage.api.operations.MutationOperations;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;
import fr.inria.corese.core.next.storage.api.operations.StatementSpliterator;
import fr.inria.corese.core.next.storage.api.transaction.IsolationLevel;
import fr.inria.corese.core.next.storage.api.transaction.Transaction;
import fr.inria.corese.core.next.storage.api.transaction.TransactionManager;
import fr.inria.corese.core.next.storage.api.transaction.TransactionState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Session-scoped operations over a shared, partitioned in-memory database.
 *
 * <p>
 * The database monitor protects publication, transactions and partition ownership counts. Each live {@code State} owns
 * one reference to each partition in its graph registry. A transaction or lazy read captures its own state and
 * increments those references. Before mutating a shared partition, an owner copies all three index containers and
 * transfers its reference to the private copy. Immutable statements and term keys remain shared. CLEAR replaces a
 * partition with an empty partition; old readers retain the original version. Graph ordinals preserve registry order,
 * and empty named graphs remain visible until DROP.
 *
 * <p>
 * A lazy read owns its captured state independently of the session or transaction that created it. Traversal and
 * consumer callbacks run outside the database monitor over unchanged partitions. Releasing a cursor or transaction
 * decrements references under the monitor; the last owner allows the old containers to be reclaimed. Stream consumers
 * must close their streams, including on early exit or exception.
 *
 * <p>
 * Batch ingestion stages a single pass over the source before atomically publishing completed partitions. A failed
 * batch leaves the visible state unchanged. Dirty transactions publish only if the committed database version still
 * matches the version captured at begin; a conflict leaves rollback available to the caller.
 */
final class IndexedStatementStore
        implements QueryOperations, MutationOperations, MetadataOperations, TransactionManager {

    /** Allocates unique content versions without adding fields to retained database states. */
    private static final AtomicLong REVISIONS = new AtomicLong();
    // One batch-wide reservation: at most 65,536 HashMap buckets, even for many graphs.
    /**
     * Upper bound on the batch-wide initial canonicalization reservation.
     */
    private static final int MAX_INITIAL_STAGING_TERMS = 49_152;
    /**
     * Shared lifecycle monitor and committed database state.
     */
    private final Database database;
    /**
     * Active transaction owned by this session, or null.
     */
    private SnapshotTransaction transaction;
    /**
     * Whether this session has relinquished its operation lifecycle.
     */
    private boolean closed;
    /**
     * Whether this session has an atomic batch in progress.
     */
    private boolean loading;

    /** One graph and its three permutations; references count State owners, not rows. */
    private static final class Partition {
        // Number of live States owning this partition, guarded by the database monitor.
        /**
         * Number of live State owners, guarded by the database monitor.
         */
        private int references = 1;
        /**
         * Stable graph registry position.
         */
        private final long ordinal;
        /**
         * Canonical graph identifier; null denotes the default graph.
         */
        private final Resource context;
        /**
         * Subject/predicate/object permutation of this graph.
         */
        private final TripleIndex spo;
        /**
         * Predicate/object/subject permutation of this graph.
         */
        private final TripleIndex pos;
        /**
         * Object/subject/predicate permutation of this graph.
         */
        private final TripleIndex osp;

        /**
         * Creates a graph partition owning the supplied permutations or empty indexes.
         *
         * @param context canonical graph identifier; null denotes the default graph
         * @param ordinal stable graph registry ordinal
         */
        private Partition(final Resource context, final long ordinal) {
            this(context, ordinal, new TripleIndex(TripleIndex.Order.SPO), new TripleIndex(TripleIndex.Order.POS),
                    new TripleIndex(TripleIndex.Order.OSP));
        }

        /**
         * Creates a graph partition owning the supplied permutations or empty indexes.
         *
         * @param context canonical graph identifier; null denotes the default graph
         * @param ordinal stable graph registry ordinal
         * @param spo     SPO index owning immutable statements and canonical keys
         * @param pos     POS permutation of the same graph partition
         * @param osp     OSP permutation of the same graph partition
         */
        private Partition(final Resource context, final long ordinal, final TripleIndex spo, final TripleIndex pos,
                final TripleIndex osp) {
            this.context = context;
            this.ordinal = ordinal;
            this.spo = spo;
            this.pos = pos;
            this.osp = osp;
        }

        /**
         * Constructs a complete partition and releases its SPO staging dictionary before publication.
         *
         * @param spo     SPO index owning immutable statements and canonical keys
         * @param context canonical graph identifier; null denotes the default graph
         * @param ordinal stable graph registry ordinal
         * @return a complete graph partition with no retained staging dictionary
         */
        private static Partition build(final TripleIndex spo, final Resource context, final long ordinal) {
            final TripleIndex.SecondaryIndexes secondary = TripleIndex.buildSecondaryIndexes(spo);
            final Partition partition = new Partition(context, ordinal, spo, secondary.pos(), secondary.osp());
            spo.finishStaging();
            return partition;
        }

        /**
         * Copies owned mutable containers while sharing immutable keys and statements.
         *
         * @return an independently owned copy of the mutable containers
         */
        private Partition copy() {
            return new Partition(context, ordinal, spo.copy(), pos.copy(), osp.copy());
        }

        /**
         * Adds a statement or keyed value if it is absent.
         *
         * @param s captured statement to index
         * @return true when an absent entry was added, false when it was already present
         */
        private boolean add(Statement s) {
            final TermKey subject = key(s.getSubject(), null, null, spo, pos, osp);
            final TermKey predicate = key(s.getPredicate(), subject, null, pos, spo, osp);
            final TermKey object = key(s.getObject(), subject, predicate, osp, spo, pos);
            s = StatementCapture.sharedStatement(s, subject, predicate, object, context);
            if (!spo.add(s, subject, predicate, object, osp)) {
                return false;
            }
            pos.add(s, subject, predicate, object, spo);
            osp.add(s, subject, predicate, object, pos);
            return true;
        }

        /**
         * Reuses a canonical RDF term key across roles, creating one only for a previously unseen term.
         *
         * @param value     non-null RDF value or entry value
         * @param subject   canonical subject key, or null for a wildcard search
         * @param predicate canonical predicate key; null denotes a wildcard for search operations
         * @param primary   preferred role-root index for canonical lookup
         * @param secondary second role-root index for canonical lookup
         * @param tertiary  third role-root index for canonical lookup
         * @return a reusable canonical key, or a newly captured key for an unseen RDF term
         */
        private TermKey key(final Value value, final TermKey subject, final TermKey predicate,
                final TripleIndex primary, final TripleIndex secondary, final TripleIndex tertiary) {
            final int hash = TermKey.structuralHash(value);
            if (subject != null && subject.matchesLookup(value, hash)) {
                return subject;
            }
            if (predicate != null && predicate.matchesLookup(value, hash)) {
                return predicate;
            }
            // Role roots retain canonical keys; absent RDF terms require a new key.
            TermKey canonical = primary.existingKey(value, hash);
            if (canonical == null) {
                canonical = secondary.existingKey(value, hash);
            }
            if (canonical == null) {
                canonical = tertiary.existingKey(value, hash);
            }
            return canonical == null ? new TermKey(value, hash) : canonical;
        }

        /**
         * Removes matching content while preserving the order of survivors.
         *
         * @param s captured statement to index
         * @return true when an entry was removed, false when it was absent
         */
        private boolean remove(final Statement s) {
            final TermKey subject = new TermKey(s.getSubject());
            final TermKey predicate = new TermKey(s.getPredicate());
            final TermKey object = new TermKey(s.getObject());
            if (!spo.remove(subject, predicate, object)) {
                return false;
            }
            pos.remove(subject, predicate, object);
            osp.remove(subject, predicate, object);
            return true;
        }

        /**
         * Removes selected index content with stable filtering and unchanged compaction rules.
         *
         * @param pattern captured search pattern
         */
        private void removeMatches(final StatementPattern pattern) {
            final TermKey subject = TermKey.nullable(pattern.getSubject());
            final TermKey predicate = TermKey.nullable(pattern.getPredicate());
            final TermKey object = TermKey.nullable(pattern.getObject());
            spo.removeMatches(subject, predicate, object);
            pos.removeMatches(subject, predicate, object);
            osp.removeMatches(subject, predicate, object);
        }

        /**
         * Selects the permutation appropriate for the bound prefix of a search pattern.
         *
         * @param p captured search pattern
         * @return the SPO, POS or OSP permutation selected for this pattern
         */
        private TripleIndex index(final StatementPattern p) {
            if (p.getSubject() != null && (p.getPredicate() != null || p.getObject() == null)) {
                return spo;
            }
            if (p.getPredicate() != null) {
                return pos;
            }
            return p.getObject() != null ? osp : spo;
        }

        /**
         * Reports whether the session-visible or retained graph content contains a match.
         *
         * @param statement statement whose RDF identities have been captured
         * @return true when at least one matching statement exists
         */
        private boolean contains(final Statement statement) {
            return spo.count(
                    StatementPattern.of(statement.getSubject(), statement.getPredicate(), statement.getObject())) != 0;
        }
    }

    /** Ordered graph registry whose partition references are acquired and released together. */
    private static final class State {

        /** Creates an empty ordered registry with exclusive ownership of its map. */
        private State() {
        }

        // A null key is the default partition. Empty named partitions are retained until DROP.
        /**
         * Ordered partition registry; empty named graphs are retained until DROP.
         */
        private final Map<Resource, Partition> graphs = new LinkedHashMap<>();
        /**
         * Number of live entries or statements owned by this container.
         */
        private long size;
        /**
         * Ordinal assigned to the next created graph.
         */
        private long nextOrdinal;

        /**
         * Copies owned mutable containers while sharing immutable keys and statements.
         *
         * @return an independently owned copy of the mutable containers
         */
        private State copy() {
            final State copy = new State();
            graphs.forEach((graph, partition) -> {
                partition.references++;
                copy.graphs.put(graph, partition);
            });
            copy.size = size;
            copy.nextOrdinal = nextOrdinal;
            return copy;
        }

        /**
         * Capture selected graphs only; writes to unrelated graphs need no cursor-induced COW.
         *
         * @param contexts selected graph identifiers; an empty array selects all graphs
         * @return an independently owned copy of the mutable containers
         */
        private State copy(final Resource[] contexts) {
            if (contexts.length == 0) return copy();
            final State copy = new State();
            final List<Partition> selected = new ArrayList<>();
            for (final Resource graph : contexts) {
                final Partition partition = graphs.get(graph);
                if (partition != null) selected.add(partition);
            }
            selected.sort(Comparator.comparingLong(partition -> partition.ordinal));
            for (final Partition partition : selected) {
                partition.references++;
                copy.graphs.put(partition.context, partition);
                copy.size += partition.spo.size();
            }
            copy.nextOrdinal = nextOrdinal;
            return copy;
        }

        /**
         * Detaches only a shared partition, preserving existing readers and graph order.
         *
         * @param graph named graph identifier or staging registry key, according to the operation
         * @return the exclusively owned partition, or null if the graph is absent
         */
        private Partition writable(final Resource graph) {
            final Partition partition = graphs.get(graph);
            if (partition != null && partition.references > 1) {
                final Partition replacement = partition.copy();
                partition.references--;
                graphs.put(graph, replacement);
                return replacement;
            }
            return partition;
        }

        /**
         * Replaces a partition with an empty version, preserving graph identity and registry position.
         *
         * @param partition partition retained or exclusively owned by the supplied state
         */
        private void empty(final Partition partition) {
            // Keep the graph's identity and registry position. Readers continue to own
            // the old partition; clearing never needs to clone its three indexes.
            final Partition replacement = new Partition(partition.context, partition.ordinal);
            graphs.put(partition.context, replacement);
            partition.references--;
        }

        /** Releases this state's ownership; clearing the registry makes repeated release harmless. */
        private void release() {
            graphs.values().forEach(partition -> partition.references--);
            graphs.clear();
        }
    }

    /**
     * Shared database state and lifecycle monitor for all sessions.
     */
    private static final class Database implements StorageLifecycle {

        /** Creates an uninitialized database and its exclusively owned empty committed state. */
        private Database() {
        }

        /**
         * Committed state whose partition references are owned by the database.
         */
        private State committed = new State();
        /**
         * Committed version captured for optimistic conflict detection.
         */
        private long version;
        /**
         * Current shared database lifecycle status.
         */
        private LifecycleState lifecycleState = LifecycleState.NOT_INITIALIZED;
        /**
         * Configuration available during the running lifecycle.
         */
        private StorageConfig config;

        /**
         * {@inheritDoc}
         *
         * @param config storage configuration selecting this backend
         * @throws IllegalStateException if the required representation, ownership or lifecycle state is not satisfied
         * @throws NullPointerException  if an explicitly required argument or mapped value is null
         */
        @Override
        public synchronized void initialize(final StorageConfig config) {
            Objects.requireNonNull(config, "config");
            if (lifecycleState != LifecycleState.NOT_INITIALIZED) {
                throw new IllegalStateException("Already initialized");
            }
            this.config = config;
            lifecycleState = LifecycleState.RUNNING;
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalStateException if the required representation, ownership or lifecycle state is not satisfied
         */
        @Override
        public synchronized void shutdown() {
            if (lifecycleState != LifecycleState.RUNNING) {
                throw new IllegalStateException("Storage is not running");
            }
            committed.release();
            committed = new State();
            config = null;
            lifecycleState = LifecycleState.SHUTDOWN;
        }

        /**
         * {@inheritDoc}
         *
         * @return the current lifecycle or transaction status
         */
        @Override
        public synchronized LifecycleState getState() {
            return lifecycleState;
        }

        /**
         * {@inheritDoc}
         *
         * @return current configuration when the database is running
         */
        @Override
        public synchronized Optional<StorageConfig> getConfig() {
            return Optional.ofNullable(config);
        }
    }

    /**
     * Creates session operations sharing the supplied database, or a new uninitialized database.
     */
    IndexedStatementStore() {
        this(new Database());
    }

    /**
     * Creates session operations sharing the supplied database, or a new uninitialized database.
     *
     * @param database shared database monitor and state
     */
    private IndexedStatementStore(final Database database) {
        this.database = database;
    }

    /**
     * Returns the shared database lifecycle controller.
     *
     * @return the shared database lifecycle controller
     */
    StorageLifecycle lifecycle() {
        return database;
    }

    /**
     * Reports whether this session and the shared database accept operations.
     *
     * @return true when the session is open and the database is running
     */
    boolean isOpen() {
        synchronized (database) {
            return !closed && database.lifecycleState == LifecycleState.RUNNING;
        }
    }

    /**
     * Creates a session sharing the database and owning its own transaction lifecycle.
     *
     * @return a new session sharing the database
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread
     */
    IndexedStatementStore openSession() {
        synchronized (database) {
            checkAccess();
            return new IndexedStatementStore(database);
        }
    }

    /** Captures the visible state without transferring ownership of an active transaction. */
    @SuppressWarnings("java:S1181") // Release retained partitions if snapshot initialization fails, then rethrow unchanged.
    IndexedStatementStore openReadSnapshot() {
        synchronized (database) {
            checkAccess();
            final IndexedStatementStore snapshot = new IndexedStatementStore(database);
            final State captured = state().copy();
            try {
                snapshot.transaction = snapshot.new SnapshotTransaction(captured, getDataVersion(), true);
                return snapshot;
            } catch (final RuntimeException | Error failure) {
                captured.release();
                throw failure;
            }
        }
    }

    /**
     * Closes this session, rolling back its active transaction; independent read snapshots remain retained.
     *
     * @throws IllegalStateException if the required representation, ownership or lifecycle state is not satisfied
     */
    void closeSession() {
        synchronized (database) {
            if (closed) {
                return;
            }
            if (loading) {
                throw new IllegalStateException("A batch is active on this session");
            }
            if (transaction != null) {
                if (transaction.readOnly) {
                    transaction.finish(TransactionState.ROLLED_BACK);
                } else {
                    transaction.rollback();
                }
            }
            closed = true;
        }
    }

    /**
     * Requires an open session and, when active, access from its transaction owner thread.
     *
     * @throws IllegalStateException if the required representation, ownership or lifecycle state is not satisfied
     */
    private void checkAccess() {
        if (!isOpen()) {
            throw new IllegalStateException("Storage session is closed");
        }
        if (transaction != null) {
            transaction.checkOwner();
        }
    }

    /**
     * Requires session access and excludes overlapping batch mutations on this session.
     *
     * @throws IllegalStateException if the required representation, ownership or lifecycle state is not satisfied
     */
    private void checkWrite() {
        checkAccess();
        if (transaction != null && transaction.readOnly) {
            throw new IllegalStateException("Read snapshots cannot be mutated");
        }
        if (loading) {
            throw new IllegalStateException("A batch is active on this session");
        }
    }

    /**
     * Returns the session-visible state; the database monitor must be held.
     *
     * @return the transaction state, or the committed state outside a transaction
     */
    private State state() {
        return transaction == null ? database.committed : transaction.state;
    }

    /**
     * Marks a successful mutation in the current transaction or increments the committed version.
     *
     * @param changed whether this mutation changed visible content
     */
    private void changed(final boolean changed) {
        if (!changed) {
            return;
        }
        if (transaction == null) {
            database.version = REVISIONS.incrementAndGet();
        } else {
            transaction.dirty = true;
            transaction.revision = REVISIONS.incrementAndGet();
        }
    }

    /** Returns the version of this session's visible data under the database monitor. */
    @Override
    public long getDataVersion() {
        synchronized (database) {
            checkAccess();
            return transaction == null ? database.version : transaction.revision;
        }
    }

    /**
     * Captures the selected partitions immediately and traverses matches lazily.
     *
     * <p>
     * Runtime failures and Errors during cursor construction release the captured ownership before the original failure
     * is rethrown. Error handling is limited to resource release; no failure is recovered or converted.
     *
     * <p>
     * The returned sequential stream must be used in try-with-resources. Terminal operations do not close streams:
     * early operations such as {@code findFirst()} or {@code limit()} can retain ownership, and a sized {@code count()}
     * may skip traversal entirely. Exhaustion and traversal failures release ownership as an additional safeguard;
     * explicit close remains the caller's responsibility.
     *
     * @param pattern captured search pattern
     * @return a sequential stream owning its read snapshot; the caller must close it
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread
     */
    @SuppressWarnings("java:S1181")
    @Override
    public Stream<Statement> find(StatementPattern pattern) {
        pattern = StatementCapture.immutable(pattern);
        synchronized (database) {
            checkAccess();
            final State snapshot = state().copy(pattern.getContexts());
            try {
                var cursor = new SnapshotCursor(snapshot, pattern);
                return StreamSupport.stream(cursor, false).onClose(cursor::close);
            } catch (final RuntimeException | Error failure) {
                snapshot.release();
                throw failure;
            }
        }
    }

    /**
     * Single-consumer cursor owning a captured state until close, exhaustion or failure. Known-size traversal can
     * release ownership before delivering the last immutable statement. Close is idempotent and clears traversal
     * references under the monitor. No database monitor is held while visiting rows or invoking consumer code. Session
     * and transaction closure do not invalidate this independent read snapshot.
     */
    private final class SnapshotCursor implements StatementSpliterator, AutoCloseable {
        /**
         * Read state retained until this cursor is closed or exhausted.
         */
        private State snapshot;
        /**
         * Ordered traversal of the retained partition registry.
         */
        private Iterator<Partition> partitions;
        /**
         * Positional traversal of the current retained partition.
         */
        private TripleIndex.Cursor rows;
        /**
         * Captured immutable search pattern.
         */
        private final StatementPattern pattern;
        /**
         * Canonical traversal subject or current batch subject, according to the enclosing traversal.
         */
        private final TermKey subject;
        /**
         * Canonical traversal predicate or current batch predicate, according to the enclosing traversal.
         */
        private final TermKey predicate;
        /**
         * Captured object search key; null denotes a wildcard.
         */
        private final TermKey object;
        /**
         * Whether the retained match count is known exactly.
         */
        private final boolean sized;
        /**
         * Snapshot-bound guarantee of SPO uniqueness independently of context.
         */
        private final boolean distinctTriples;
        /**
         * Remaining exact count or the unsized upper bound while this cursor is open.
         */
        private long remaining;

        /**
         * Creates a single-consumer cursor owning the supplied retained read state.
         *
         * @param snapshot retained read state
         * @param pattern  captured search pattern
         */
        private SnapshotCursor(final State snapshot, final StatementPattern pattern) {
            this.snapshot = snapshot;
            this.pattern = pattern;
            distinctTriples = hasAtMostOnePopulatedPartition(snapshot);
            subject = TermKey.nullable(pattern.getSubject());
            predicate = TermKey.nullable(pattern.getPredicate());
            object = TermKey.nullable(pattern.getObject());
            sized = subject == null && predicate == null && object == null;
            remaining = sized ? snapshot.size : Long.MAX_VALUE;
            partitions = snapshot.graphs.values().iterator();
        }

        /**
         * {@inheritDoc}
         *
         * @param action consumer invoked outside the database monitor
         * @return true when a statement was delivered, false when this cursor is closed or exhausted
         * @throws NullPointerException if an explicitly required argument or mapped value is null
         */
        // Traversal Errors release ownership and are rethrown unchanged.
        @SuppressWarnings("java:S1181")
        @Override
        public boolean tryAdvance(final Consumer<? super Statement> action) {
            Objects.requireNonNull(action);
            if (snapshot == null) {
                return false;
            }
            try {
                for (;;) {
                    final Statement statement = rows == null ? null : rows.next();
                    if (statement != null) {
                        if (sized && --remaining == 0) {
                            close();
                        }
                        action.accept(statement);
                        return true;
                    }
                    if (!partitions.hasNext()) {
                        close();
                        return false;
                    }
                    rows = partitions.next().index(pattern).cursor(subject, predicate, object);
                }
            } catch (final RuntimeException | Error failure) {
                close();
                throw failure;
            }
        }

        /**
         * {@inheritDoc}
         *
         * @param action consumer invoked outside the database monitor
         * @throws NullPointerException if an explicitly required argument or mapped value is null
         */
        @Override
        public void forEachRemaining(final Consumer<? super Statement> action) {
            Objects.requireNonNull(action);
            if (snapshot == null) {
                return;
            }
            // A complete scan uses the existing bulk visitor, without per-row cursor overhead.
            if (rows != null) {
                StatementSpliterator.super.forEachRemaining(action);
                return;
            }
            try {
                final Predicate<Statement> visitor = statement -> {
                    if (sized && --remaining == 0) {
                        close();
                    }
                    action.accept(statement);
                    return snapshot == null;
                };
                while (snapshot != null && partitions.hasNext()) {
                    partitions.next().index(pattern).visit(subject, predicate, object, visitor);
                }
            } finally {
                close();
            }
        }

        /**
         * {@inheritDoc}
         *
         * @return true only when SPO uniqueness is guaranteed for this retained read
         */
        @Override
        public boolean hasDistinctTriples() {
            return distinctTriples;
        }

        /**
         * {@inheritDoc}
         *
         * @return null; this traversal cannot be split
         */
        @Override
        public Spliterator<Statement> trySplit() {
            return null;
        }

        /**
         * {@inheritDoc}
         *
         * @return remaining size estimate, or zero once the cursor is closed
         */
        @Override
        public long estimateSize() {
            return snapshot == null ? 0 : remaining;
        }

        /**
         * {@inheritDoc}
         *
         * @return the ORDERED, NONNULL and IMMUTABLE flags, plus SIZED and SUBSIZED when the exact count is known
         */
        @Override
        public int characteristics() {
            return ORDERED | NONNULL | IMMUTABLE | (sized ? SIZED | SUBSIZED : 0);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
            synchronized (database) {
                if (snapshot == null) {
                    return;
                }
                snapshot.release();
                snapshot = null;
                partitions = null;
                rows = null;
            }
        }
    }

    /**
     * Empty named partitions survive CLEAR but cannot contribute duplicate triples.
     *
     * @param snapshot retained read state
     * @return true when at most one selected partition contains statements
     */
    private static boolean hasAtMostOnePopulatedPartition(final State snapshot) {
        if (snapshot.graphs.size() <= 1) {
            return true;
        }
        boolean populated = false;
        for (final Partition partition : snapshot.graphs.values()) {
            if (partition.spo.size() == 0) {
                continue;
            }
            if (populated) {
                return false;
            }
            populated = true;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @param pattern captured search pattern
     * @return matching statement count
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread
     */
    @Override
    public long count(StatementPattern pattern) {
        pattern = StatementCapture.immutable(pattern);
        synchronized (database) {
            checkAccess();
            if (pattern.matchesAll()) {
                return state().size;
            }
            final State current = state();
            if (current.graphs.isEmpty()) {
                return 0;
            }
            final Resource[] contexts = pattern.getContexts();
            // Keep one-partition keys local to the index for JVM scalar replacement.
            if (contexts.length == 1) {
                final Partition partition = current.graphs.get(contexts[0]);
                return partition == null ? 0 : partition.index(pattern).count(pattern);
            }
            if (contexts.length == 0 && current.graphs.size() == 1) {
                final Partition partition = current.graphs.values().iterator().next();
                return partition.index(pattern).count(pattern);
            }
            return countSelectedPartitions(current, contexts, pattern);
        }
    }

    /**
     * Counts selected graph matches using one set of call-local search keys.
     *
     * @param current  session-visible state guarded by the database monitor
     * @param contexts selected graph identifiers; an empty array selects all graphs
     * @param pattern  captured search pattern
     * @return number of matching statements across selected graph partitions
     */
    private static long countSelectedPartitions(final State current, final Resource[] contexts,
            final StatementPattern pattern) {
        // Call-local keys: no per-partition hashing or retained query cache.
        final TermKey subject = TermKey.nullable(pattern.getSubject());
        final TermKey predicate = TermKey.nullable(pattern.getPredicate());
        final TermKey object = TermKey.nullable(pattern.getObject());
        long count = 0;
        if (contexts.length == 0) {
            for (final Partition partition : current.graphs.values()) {
                count += partition.index(pattern).count(subject, predicate, object);
            }
        } else {
            for (final Resource context : contexts) {
                final Partition partition = current.graphs.get(context);
                if (partition != null) {
                    count += partition.index(pattern).count(subject, predicate, object);
                }
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     *
     * @param pattern captured search pattern
     * @return true when at least one matching statement exists
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread
     */
    @Override
    public boolean contains(StatementPattern pattern) {
        pattern = StatementCapture.immutable(pattern);
        synchronized (database) {
            checkAccess();
            return visit(state(), pattern, s -> true);
        }
    }

    /**
     * Traverses matching statements or live values until the visitor requests an early stop.
     *
     * @param state   state whose ownership is guarded by the database monitor
     * @param pattern captured search pattern
     * @param visitor non-mutating callback; true requests an early stop for predicate visitors
     * @return true when a matching value caused the visitor to request an early stop
     */
    private static boolean visit(final State state, final StatementPattern pattern,
            final Predicate<Statement> visitor) {
        if (state.graphs.isEmpty()) {
            return false;
        }
        final Resource[] contexts = pattern.getContexts();
        if (contexts.length == 1) {
            final Partition partition = state.graphs.get(contexts[0]);
            return partition != null && partition.index(pattern).visit(pattern, visitor);
        }
        if (contexts.length == 0 && state.graphs.size() == 1) {
            final Partition partition = state.graphs.values().iterator().next();
            return partition.index(pattern).visit(pattern, visitor);
        }
        return visitSelectedPartitions(state, pattern, visitor, contexts);
    }

    /**
     * Visits selected graphs using call-local keys and stable registry order.
     *
     * @param state    state whose ownership is guarded by the database monitor
     * @param pattern  captured search pattern
     * @param visitor  non-mutating callback; true requests an early stop for predicate visitors
     * @param contexts selected graph identifiers; an empty array selects all graphs
     * @return true when the visitor requests an early stop on a matching statement
     */
    private static boolean visitSelectedPartitions(final State state, final StatementPattern pattern,
            final Predicate<Statement> visitor, final Resource[] contexts) {
        final TermKey subject = TermKey.nullable(pattern.getSubject());
        final TermKey predicate = TermKey.nullable(pattern.getPredicate());
        final TermKey object = TermKey.nullable(pattern.getObject());
        if (contexts.length == 0) {
            for (final Partition partition : state.graphs.values()) {
                if (partition.index(pattern).visit(subject, predicate, object, visitor)) {
                    return true;
                }
            }
        } else {
            final List<Partition> partitions = selectPartitions(state, contexts);
            for (final Partition partition : partitions) {
                if (partition.index(pattern).visit(subject, predicate, object, visitor)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Materializes selected partitions in registry order; the database monitor must be held.
     *
     * @param state    state whose ownership is guarded by the database monitor
     * @param contexts selected graph identifiers; an empty array selects all graphs
     * @return existing selected partitions in stable registry order
     */
    private static List<Partition> selectPartitions(final State state, final Resource[] contexts) {
        // Preserve registry order independently of the caller's context ordering.
        final List<Partition> partitions = new ArrayList<>();
        for (final Resource context : contexts) {
            final Partition partition = state.graphs.get(context);
            if (partition != null) {
                partitions.add(partition);
            }
        }
        partitions.sort(Comparator.comparingLong(p -> p.ordinal));
        return partitions;
    }

    /**
     * {@inheritDoc}
     *
     * @param statement statement whose RDF identities have been captured
     * @return true when an absent entry was added, false when it was already present
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread, or a
     *                               conflicting batch is active
     */
    @Override
    public boolean add(final Statement statement) {
        final Statement captured = StatementCapture.immutable(statement);
        synchronized (database) {
            checkWrite();
            final State current = state();
            final Partition existing = current.graphs.get(captured.getContext());
            if (existing != null && existing.references > 1 && existing.contains(captured)) {
                return false;
            }
            Partition partition = current.writable(captured.getContext());
            if (partition == null) {
                partition = new Partition(captured.getContext(), current.nextOrdinal++);
                current.graphs.put(captured.getContext(), partition);
            }
            final boolean added = partition.add(captured);
            if (added) {
                state().size++;
            }
            changed(added);
            return added;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param statement statement whose RDF identities have been captured
     * @return true when an entry was removed, false when it was absent
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread, or a
     *                               conflicting batch is active
     */
    @Override
    public boolean remove(final Statement statement) {
        final Statement captured = StatementCapture.immutable(statement);
        synchronized (database) {
            checkWrite();
            final Partition existing = state().graphs.get(captured.getContext());
            if (existing == null || (existing.references > 1 && !existing.contains(captured))) {
                return false;
            }
            final Partition partition = state().writable(captured.getContext());
            final boolean removed = partition != null && partition.remove(captured);
            if (removed) {
                state().size--;
            }
            changed(removed);
            return removed;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param pattern captured search pattern
     * @return number of matching statements removed
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread, or a
     *                               conflicting batch is active
     */
    @Override
    public long remove(StatementPattern pattern) {
        pattern = StatementCapture.immutable(pattern);
        synchronized (database) {
            checkWrite();
            final State current = state();
            final Resource[] contexts = pattern.getContexts();
            long removed = 0;
            if (contexts.length == 0) {
                // Replacing an existing value does not change LinkedHashMap iteration order.
                for (final Partition partition : current.graphs.values()) {
                    removed += remove(current, partition, pattern);
                }
            } else {
                for (final Resource context : contexts) {
                    final Partition partition = current.graphs.get(context);
                    if (partition != null) {
                        removed += remove(current, partition, pattern);
                    }
                }
            }
            current.size -= removed;
            changed(removed != 0);
            return removed;
        }
    }

    /**
     * Removes matching content while preserving the order of survivors.
     *
     * @param current   session-visible state guarded by the database monitor
     * @param partition partition retained or exclusively owned by the supplied state
     * @param pattern   captured search pattern
     * @return number of matching statements removed
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread, or a
     *                               conflicting batch is active
     */
    private static long remove(final State current, final Partition partition, final StatementPattern pattern) {
        final long size = partition.spo.size();
        if (size == 0) {
            return 0;
        }
        final long count = pattern.isSubjectWildcard() && pattern.isPredicateWildcard() && pattern.isObjectWildcard()
                ? size
                : partition.index(pattern).count(pattern);
        if (count == 0) {
            return 0;
        }
        if (count == size) {
            current.empty(partition);
        } else if (count >= 64 && count >= size / 4) {
            // Broad deletions filter each index once; selective ones keep indexed seeks.
            current.writable(partition.context).removeMatches(pattern);
        } else {
            final List<Statement> matches = new ArrayList<>();
            partition.index(pattern).visit(pattern, s -> {
                matches.add(s);
                return false;
            });
            final Partition writable = current.writable(partition.context);
            for (final Statement statement : matches) {
                writable.remove(statement);
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     *
     * @param contexts selected graph identifiers; an empty array selects all graphs
     * @return number of removed statements
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread, or a
     *                               conflicting batch is active
     */
    @Override
    public long clear(final Resource... contexts) {
        return remove(StatementPattern.of(null, null, null, contexts));
    }

    /**
     * {@inheritDoc}
     *
     * @param graph named graph identifier or staging registry key, according to the operation
     * @return true when the named graph was created
     * @throws NullPointerException  if an explicitly required argument or mapped value is null
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread, or a
     *                               conflicting batch is active
     */
    @Override
    public boolean createGraph(final Resource graph) {
        final Resource captured = (Resource) StatementCapture.immutable(Objects.requireNonNull(graph, "graph"));
        synchronized (database) {
            checkWrite();
            if (state().graphs.containsKey(captured)) {
                return false;
            }
            state().graphs.put(captured, new Partition(captured, state().nextOrdinal++));
            changed(true);
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param graph named graph identifier or staging registry key, according to the operation
     * @return true when the named graph existed and was dropped
     * @throws NullPointerException  if an explicitly required argument or mapped value is null
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread, or a
     *                               conflicting batch is active
     */
    @Override
    public boolean dropGraph(final Resource graph) {
        final Resource captured = (Resource) StatementCapture.immutable(Objects.requireNonNull(graph, "graph"));
        synchronized (database) {
            checkWrite();
            final Partition removed = state().graphs.remove(captured);
            if (removed == null) {
                return false;
            }
            removed.references--;
            state().size -= removed.spo.size();
            changed(true);
            return true;
        }
    }

    /**
     * Atomic finite batch, including graph creation; source is consumed once outside the database lock.
     *
     * @param statements finite source consumed in exactly one pass
     * @return number of newly added statements across the atomic batch
     * @throws StorageException      if publication fails or a concurrent committed version prevents commit
     * @throws NullPointerException  if an explicitly required argument or mapped value is null
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread, or a
     *                               conflicting batch is active
     */
    @Override
    public long addAll(final Iterable<? extends Statement> statements) {
        Objects.requireNonNull(statements, "statements");
        synchronized (database) {
            checkWrite();
            loading = true;
        }
        try {
            return publishBatch(stageStatements(statements));
        } catch (final NullPointerException failure) {
            throw failure;
        } catch (final RuntimeException failure) {
            throw new StorageException(ErrorCode.MUTATION_FAILED, "Atomic batch failed", failure);
        } finally {
            synchronized (database) {
                loading = false;
            }
        }
    }

    /**
     * Captures one pass over the source in private SPO staging; no database monitor is held.
     *
     * @param statements finite source consumed in exactly one pass
     * @return private graph-local SPO staging in source encounter order
     */
    private static Map<Resource, StagedPartition> stageStatements(final Iterable<? extends Statement> statements) {
        final int expectedTerms = stagingTermHint(statements);
        final Map<Resource, StagedPartition> staged = new LinkedHashMap<>();
        final Function<Resource, StagedPartition> newPartition = graph -> new StagedPartition(graph,
                TripleIndex.forStaging(staged.isEmpty() ? expectedTerms : 0));
        for (final Statement statement : statements) {
            final Statement captured = StatementCapture.immutable(statement);
            final StagedPartition partition = staged.computeIfAbsent(captured.getContext(), newPartition);
            partition.statements.add(captured, partition.context);
        }
        return staged;
    }

    /**
     * Merges completed staging and atomically publishes it under the database monitor.
     *
     * @param staged unpublished graph-local SPO staging
     * @return number of newly added statements
     */
    private long publishBatch(final Map<Resource, StagedPartition> staged) {
        synchronized (database) {
            checkAccess();
            final State target = state();
            final State replacement = target.copy();
            boolean published = false;
            try {
                final long[] added = { 0 };
                staged.forEach((graph, stagedPartition) -> {
                    added[0] += mergeStagedPartition(target, replacement, graph, stagedPartition);
                });
                if (added[0] != 0) {
                    replacement.size += added[0];
                    target.release();
                    if (transaction == null) {
                        database.committed = replacement;
                    } else {
                        transaction.state = replacement;
                    }
                    published = true;
                    changed(true);
                }
                return added[0];
            } finally {
                if (!published) {
                    replacement.release();
                }
            }
        }
    }

    /**
     * Builds one changed partition in the unpublished replacement state.
     *
     * @param target          current state being merged without mutating its retained partitions
     * @param replacement     unpublished replacement state owning the changed partitions
     * @param graph           named graph identifier or staging registry key, according to the operation
     * @param stagedPartition private SPO staging for this graph
     * @return number of new statements in this graph
     */
    private static long mergeStagedPartition(final State target, final State replacement, final Resource graph,
            final StagedPartition stagedPartition) {
        final TripleIndex additions = stagedPartition.statements;
        final Partition existing = target.graphs.get(graph);
        final TripleIndex merged = existing == null ? additions : existing.spo.copy();
        final long before = merged.size();
        if (existing != null) {
            additions.visit(StatementPattern.matchAll(), s -> {
                merged.add(s, existing.context);
                return false;
            });
        }
        final long count = existing == null ? merged.size() : merged.size() - before;
        if (count != 0) {
            final Partition built = Partition.build(merged, existing == null ? graph : existing.context,
                    existing == null ? replacement.nextOrdinal++ : existing.ordinal);
            if (existing != null) {
                existing.references--;
            }
            replacement.graphs.put(graph, built);
        }
        return count;
    }

    /**
     * Computes a bounded reservation from an available Collection size without iterating the source.
     *
     * @param statements finite source consumed in exactly one pass
     * @return bounded term reservation, or zero when the size is unavailable
     */
    private static int stagingTermHint(final Iterable<? extends Statement> statements) {
        if (statements instanceof Collection<?> collection) {
            try {
                return (int) Math.min(MAX_INITIAL_STAGING_TERMS, 3L * collection.size());
            } catch (final UnsupportedOperationException ignored) {
                /* Some streaming collections cannot report a size. */ }
        }
        return 0;
    }

    /**
     * Batch-local context and SPO staging; never published or shared with reader states.
     *
     * @param context    canonical graph identifier; null denotes the default graph
     * @param statements unpublished SPO staging
     */
    private record StagedPartition(/**
                                    * Canonical graph identifier; null denotes the default graph.
                                    */
    Resource context, /**
                       * Unpublished graph-local SPO staging.
                       */
    TripleIndex statements) {
    }

    /**
     * {@inheritDoc}
     *
     * @return an immutable ordered set of named graph identifiers
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread
     */
    @Override
    public Set<Resource> getContexts() {
        synchronized (database) {
            checkAccess();
            final Set<Resource> contexts = new LinkedHashSet<>(state().graphs.keySet());
            contexts.remove(null);
            return Collections.unmodifiableSet(contexts);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return an immutable ordered set of distinct subjects
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread
     */
    @Override
    public Set<Resource> getSubjects() {
        synchronized (database) {
            checkAccess();
            final Set<Resource> subjects = new LinkedHashSet<>();
            state().graphs.values().forEach(p -> p.spo.firstKeys().forEach(v -> subjects.add((Resource) v)));
            return Collections.unmodifiableSet(subjects);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return an immutable ordered set of distinct predicates
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread
     */
    @Override
    public Set<IRI> getPredicates() {
        synchronized (database) {
            checkAccess();
            final Set<IRI> predicates = new LinkedHashSet<>();
            state().graphs.values().forEach(p -> p.pos.firstKeys().forEach(v -> predicates.add((IRI) v)));
            return Collections.unmodifiableSet(predicates);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return an immutable ordered set of distinct objects
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread
     */
    @Override
    public Set<Value> getObjects() {
        synchronized (database) {
            checkAccess();
            final Set<Value> objects = new LinkedHashSet<>();
            state().graphs.values().forEach(p -> objects.addAll(p.osp.firstKeys()));
            return Collections.unmodifiableSet(objects);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return exact cardinalities of the session-visible state
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread
     */
    @Override
    public StorageStatistics getStatistics() {
        synchronized (database) {
            checkAccess();
            return new StorageStatistics(state().size, getSubjects().size(), getPredicates().size(),
                    getObjects().size(), getContexts().size());
        }
    }

    @Override
    public PredicateStatistics getPredicateStatistics(IRI predicate) {
        if (predicate == null) {
            return null;
        }
        synchronized (database) {
            checkAccess();
            long totalCount = 0;
            long distinctSubjects = 0;
            long distinctObjects = 0;
            boolean found = false;

            for (Partition p : state().graphs.values()) {
                PredicateStatistics ps = p.pos.estimatePredicateStatistics(predicate, 512);
                if (ps != null) {
                    found = true;
                    totalCount += ps.count();
                    distinctSubjects += ps.distinctSubjects();
                    distinctObjects += ps.distinctObjects();
                }
            }
            if (!found) {
                return null;
            }
            return new PredicateStatistics(predicate, totalCount,
                    Math.min(totalCount, distinctSubjects),
                    Math.min(totalCount, distinctObjects));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return true
     */
    @Override
    public boolean supportsTransactions() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return the immutable set containing SERIALIZABLE
     */
    @Override
    public Set<IsolationLevel> getSupportedIsolationLevels() {
        return Set.of(IsolationLevel.SERIALIZABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @return the active transaction owned by the calling thread
     * @throws IllegalStateException if the session is closed or its transaction belongs to another thread, or a
     *                               conflicting batch is active
     */
    @Override
    public Transaction beginTransaction() {
        return beginTransaction(IsolationLevel.SERIALIZABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param level requested supported isolation level
     * @return the active transaction owned by the calling thread
     * @throws IllegalArgumentException if a capacity, term type or requested isolation level is unsupported
     * @throws IllegalStateException    if the required representation, ownership or lifecycle state is not satisfied
     */
    @Override
    public Transaction beginTransaction(final IsolationLevel level) {
        if (!getSupportedIsolationLevels().contains(level)) {
            throw new IllegalArgumentException("Unsupported isolation level: " + level);
        }
        synchronized (database) {
            checkWrite();
            if (transaction != null) {
                throw new IllegalStateException("A transaction is already active on this session");
            }
            transaction = new SnapshotTransaction(database.committed.copy(), database.version);
            return transaction;
        }
    }

    /**
     * Thread-owned optimistic transaction retaining a captured state until commit or rollback.
     */
    private final class SnapshotTransaction implements Transaction {
        /**
         * Stable transaction identifier.
         */
        private final String id = UUID.randomUUID().toString();
        /**
         * Owning manager flag or transaction thread, according to the enclosing class.
         */
        private final Thread owner = Thread.currentThread();
        /**
         * Committed version captured for optimistic conflict detection.
         */
        private final long version;
        /** Version of the transaction's own visible content, independent of later commits. */
        private long revision;
        /** Independent query snapshots never publish mutations. */
        private final boolean readOnly;
        /**
         * Transaction-owned state until publication or release.
         */
        private State state;
        /**
         * Whether this transaction changed its captured state.
         */
        private boolean dirty;
        /**
         * Current transaction lifecycle status.
         */
        private TransactionState status = TransactionState.ACTIVE;

        /**
         * Creates a thread-owned transaction for the captured committed version.
         *
         * @param state   state whose ownership is guarded by the database monitor
         * @param version committed version captured for conflict detection
         */
        private SnapshotTransaction(final State state, final long version) {
            this(state, version, false);
        }

        private SnapshotTransaction(final State state, final long version, final boolean readOnly) {
            this.state = state;
            this.version = version;
            this.revision = version;
            this.readOnly = readOnly;
        }

        /**
         * Requires access from the thread that began the transaction.
         *
         * @throws IllegalStateException if the required representation, ownership or lifecycle state is not satisfied
         */
        private void checkOwner() {
            if (Thread.currentThread() != owner) {
                throw new IllegalStateException("Transaction belongs to another thread");
            }
        }

        /**
         * Requires the transaction owner, active status and no overlapping session batch.
         *
         * @throws IllegalStateException if the required representation, ownership or lifecycle state is not satisfied
         */
        private void requireActive() {
            checkOwner();
            if (status != TransactionState.ACTIVE) {
                throw new IllegalStateException("Transaction is inactive");
            }
            if (loading) {
                throw new IllegalStateException("A batch is active on this session");
            }
        }

        /**
         * {@inheritDoc}
         *
         * @return the transaction identifier
         */
        @Override
        public String getId() {
            return id;
        }

        /**
         * {@inheritDoc}
         *
         * @return true while the transaction is active
         */
        @Override
        public boolean isActive() {
            synchronized (database) {
                return status == TransactionState.ACTIVE;
            }
        }

        /**
         * {@inheritDoc}
         *
         * @return the current lifecycle or transaction status
         */
        @Override
        public TransactionState getState() {
            synchronized (database) {
                return status;
            }
        }

        /**
         * {@inheritDoc}
         *
         * @throws StorageException if publication fails or a concurrent committed version prevents commit
         */
        @Override
        public void commit() {
            synchronized (database) {
                requireActive();
                checkAccess();
                if (database.version != version) {
                    throw new StorageException(ErrorCode.TRANSACTION_ERROR,
                            "Concurrent storage modification; roll back and retry");
                }
                if (dirty) {
                    database.committed.release();
                    database.committed = state;
                    state = null; // Ownership transferred to the committed state.
                    database.version = revision;
                }
                finish(TransactionState.COMMITTED);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void rollback() {
            synchronized (database) {
                requireActive();
                finish(TransactionState.ROLLED_BACK);
            }
        }

        /**
         * Sets terminal transaction status and releases any state ownership not transferred at commit.
         *
         * @param terminal terminal transaction status
         */
        private void finish(final TransactionState terminal) {
            status = terminal;
            if (state != null) {
                state.release();
            }
            state = null;
            transaction = null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
            synchronized (database) {
                if (isActive()) {
                    rollback();
                }
            }
        }
    }

}
