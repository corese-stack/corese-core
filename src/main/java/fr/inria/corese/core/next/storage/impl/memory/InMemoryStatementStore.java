package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Value;

import java.util.Arrays;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;

import java.util.stream.Collectors;

/**
 * In-memory statement and graph store with optimistic, isolated transaction snapshots.
 *
 * <p>The transaction state is bound to the current thread. A transaction must
 * therefore be started, used, and completed by the same thread (the normal
 * repository-connection contract). Applications that transfer work between
 * executor threads must keep the connection and transaction on one worker, or
 * provide a higher-level transaction context.</p>
 *
 * <p>Beginning a transaction copies the committed statement and graph sets.
 * This gives predictable snapshot isolation, but its memory cost is
 * proportional to the dataset size. This backend is intended for tests and
 * small to medium in-memory datasets; large persistent datasets should use a
 * storage engine with native MVCC.</p>
 */
final class InMemoryStatementStore {

    private final Database database;

    private static final class Database {
        private State committed = new State(new HashSet<>(), new HashSet<>());
        private long version;
    }
    private final ThreadLocal<State> transaction = new ThreadLocal<>();

    private record State(Set<Statement> statements, Set<Resource> graphs) { }

    private State state() {
        State local = transaction.get();
        return local == null ? database.committed : local;
    }

    private void changed(boolean changed) {
        if (changed && transaction.get() == null) {
            database.version++;
        }
    }

    long begin() {
        synchronized (database) {
            if (transaction.get() != null) {
                throw new IllegalStateException("A transaction is already active on this thread");
            }
            transaction.set(new State(new HashSet<>(database.committed.statements()), new HashSet<>(database.committed.graphs())));
            return database.version;
        }
    }

    void commit(long expectedVersion) {
        synchronized (database) {
            if (database.version != expectedVersion) {
                throw new IllegalStateException("Concurrent storage modification; transaction must be rolled back");
            }
            database.committed = Objects.requireNonNull(transaction.get(), "No active transaction");
            transaction.remove();
            database.version++;
        }
    }

    void rollback() {
        transaction.remove();
    }

    public boolean createGraph(Resource graph) {
        synchronized (database) {
            boolean added = state().graphs().add(Objects.requireNonNull(graph, "graph"));
            changed(added);
            return added;
        }
    }

    public boolean dropGraph(Resource graph) {
        synchronized (database) {
            clearContext(Objects.requireNonNull(graph, "graph"));
            boolean removed = state().graphs().remove(graph);
            changed(removed);
            return removed;
        }
    }

    /**
     * Constructs a new InMemoryStatementStore with an empty statement set.
     */
    public InMemoryStatementStore() {
        this(new Database());
    }

    private InMemoryStatementStore(Database database) {
        this.database = database;
    }

    InMemoryStatementStore openSession() {
        return new InMemoryStatementStore(database);
    }

    /**
     * Adds a statement to the in-memory store.
     *
     * @param stmt the statement to add (must not be null)
     * @return true if the statement was added, false if it already existed
     */
    public boolean add(Statement stmt) {
        synchronized (database) {
            boolean graphAdded = stmt.getContext() != null && state().graphs().add(stmt.getContext());
            boolean added = state().statements().add(stmt);
            changed(added || graphAdded);
            return added;
        }
    }

    /**
     * Removes a statement from the in-memory store.
     *
     * @param stmt the statement to remove (must not be null)
     * @return true if the statement was removed, false if it did not exist
     */
    public boolean remove(Statement stmt) {
        synchronized (database) {
            boolean removed = state().statements().remove(stmt);
            changed(removed);
            return removed;
        }
    }

    /**
     * Checks if a statement exists in the in-memory store.
     *
     * @param stmt the statement to check (must not be null)
     * @return true if the statement exists
     */
    public boolean contains(Statement stmt) {
        synchronized (database) {
            return state().statements().contains(stmt);
        }
    }

    /**
     * Finds all statements matching the given pattern.
     *
     * @param s        subject filter (null = any)
     * @param p        predicate filter (null = any)
     * @param o        object filter (null = any)
     * @param contexts context filters (null/empty = any)
     * @return set of matching statements (never null, may be empty)
     */
    public Set<Statement> find(Resource s, IRI p, Value o, Resource[] contexts) {
        synchronized (database) {
            return state().statements().stream()
                    .filter(stmt -> matches(stmt, s, p, o, contexts))
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Returns the total number of statements in the store.
     *
     * @return statement count
     */
    public int size() {
        synchronized (database) {
            return state().statements().size();
        }
    }

    /**
     * Removes all statements from the store.
     */
    public void clear() {
        synchronized (database) {
            changed(!state().statements().isEmpty());
            state().statements().clear();
        }
    }

    /**
     * Removes all statements from a specific context (named graph).
     *
     * @param context the context to clear (must not be null)
     */
    public void clearContext(Resource context) {
        synchronized (database) {
            changed(state().statements().removeIf(stmt -> context.equals(stmt.getContext())));
        }
    }

    /**
     * Returns all unique subject resources in the store.
     *
     * @return set of all subjects (never null)
     */
    public Set<Resource> getSubjects() {
        synchronized (database) {
            return state().statements().stream()
                    .map(Statement::getSubject)
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Returns all unique predicate IRIs in the store.
     *
     * @return set of all predicates (never null)
     */
    public Set<IRI> getPredicates() {
        synchronized (database) {
            return state().statements().stream()
                    .map(Statement::getPredicate)
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Returns all unique object values in the store.
     *
     * @return set of all objects (never null)
     */
    public Set<Value> getObjects() {
        synchronized (database) {
            return state().statements().stream()
                    .map(Statement::getObject)
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Returns all unique context identifiers in the store.
     *
     * @return set of all contexts (never null, excludes null contexts)
     */
    public Set<Resource> getContexts() {
        synchronized (database) {
            return Set.copyOf(state().graphs());
        }
    }

    /**
     * Checks if a statement matches the given pattern.
     *
     * @param stmt     the statement to test
     * @param s        subject filter (null = wildcard)
     * @param p        predicate filter (null = wildcard)
     * @param o        object filter (null = wildcard)
     * @param contexts context filters (null/empty = wildcard)
     * @return true if the statement matches all non-null filters
     */
    private boolean matches(Statement stmt, Resource s, IRI p, Value o, Resource[] contexts) {
        if (s != null && !s.equals(stmt.getSubject())) {
            return false;
        }

        if (p != null && !p.equals(stmt.getPredicate())) {
            return false;
        }

        if (o != null && !o.equals(stmt.getObject())) {
            return false;
        }

        if (contexts != null && contexts.length > 0) {
            return matchesContext(stmt.getContext(), contexts);
        }

        return true;
    }

    /**
     * Determines whether a statement's context matches any of the provided context filters.
     *
     * <p>The comparison relies on {@link Objects#equals(Object, Object)}, ensuring
     * proper semantic equality between RDF {@link Resource} instances. In particular,
     * this preserves type distinctions (e.g., IRI vs blank node) and correctly handles
     * {@code null} values, which represent the default graph.</p>
     *
     * @param stmtContext the context of the statement, or {@code null} if it belongs
     *                    to the default graph
     * @param contexts    the context filters to match against; must not be {@code null}
     *                    or empty
     * @return {@code true} if the statement's context is equal to at least one of the
     *         provided contexts, {@code false} otherwise
     */
    private boolean matchesContext(Resource stmtContext, Resource[] contexts) {
        return Arrays.stream(contexts)
                .anyMatch(ctx -> Objects.equals(ctx, stmtContext));
    }
}
