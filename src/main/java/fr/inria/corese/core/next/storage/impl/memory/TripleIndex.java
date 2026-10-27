package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.storage.api.model.PredicateStatistics;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * One insertion-ordered SPO, POS or OSP permutation within a graph partition.
 *
 * <p>
 * The dense root table maps the first term to a compact branch keyed by the second term. A branch value is either a
 * single immutable statement or a compact leaf keyed by the third term. Singleton third keys are derived from the
 * statement when needed. All three permutations share immutable statements and canonical {@link TermKey} instances;
 * root branches retain their canonical first key so it can be reused across term roles without a persistent dictionary.
 *
 * <p>
 * Batch ingestion privately retains {@link StagingTerms} while building SPO and the secondary permutations.
 * {@link #finishStaging()} discards that dictionary before the completed partition is published. The source is read
 * once by the store; rebuilding secondary indexes traverses the staged SPO, not the source.
 *
 * <p>
 * {@link #copy()} clones root, branch and multivalued leaf buffers directly, preserving representation, holes and
 * order. Only immutable terms and statements are shared. Mutations require an exclusively owned partition; cursors
 * require their caller to retain an unchanged partition until traversal ends.
 */
final class TripleIndex {
    /**
     * Component ordering for one graph-local triple permutation.
     */
    enum Order {
        /**
         * Subject/predicate/object permutation.
         */
        SPO,
        /**
         * Predicate/object/subject permutation.
         */
        POS,
        /**
         * Object/subject/predicate permutation.
         */
        OSP;

        /**
         * Selects the third component of this permutation.
         *
         * @param s captured statement to index
         * @return the third permutation component, or null for a wildcard
         */
        Value third(final Statement s) {
            return switch (this) {
            case SPO -> s.getObject();
            case POS -> s.getSubject();
            case OSP -> s.getPredicate();
            };
        }

        /**
         * Selects the first component of this permutation.
         *
         * @param p captured search pattern
         * @return the first permutation component, or null for a wildcard
         */
        private Value first(final StatementPattern p) {
            return switch (this) {
            case SPO -> p.getSubject();
            case POS -> p.getPredicate();
            case OSP -> p.getObject();
            };
        }

        /**
         * Selects the second component of this permutation.
         *
         * @param p captured search pattern
         * @return the second permutation component, or null for a wildcard
         */
        private Value second(final StatementPattern p) {
            return switch (this) {
            case SPO -> p.getPredicate();
            case POS -> p.getObject();
            case OSP -> p.getSubject();
            };
        }

        /**
         * Selects the third component of this permutation.
         *
         * @param p captured search pattern
         * @return the third permutation component, or null for a wildcard
         */
        private Value third(final StatementPattern p) {
            return switch (this) {
            case SPO -> p.getObject();
            case POS -> p.getSubject();
            case OSP -> p.getPredicate();
            };
        }
    }

    /**
     * Component order of this triple permutation.
     */
    final Order order;
    /**
     * Insertion-ordered first-component branches.
     */
    final Map<TermKey, Branch> roots;
    // Bulk-only canonicalization cache, discarded before publishing a partition.
    /**
     * Batch-private canonical dictionary, cleared before publication.
     */
    private StagingTerms stagingTerms;
    /**
     * Number of live entries or statements owned by this container.
     */
    private long size;

    /**
     * Creates an empty permutation or copies the source permutation containers.
     *
     * @param order SPO, POS or OSP component order
     */
    TripleIndex(final Order order) {
        this.order = order;
        roots = new DenseTermTable<>();
    }

    /**
     * Creates a private SPO index with a bounded initial canonicalization reservation.
     *
     * @param expectedTerms bounded expected number of distinct staged terms
     * @return an unpublished SPO index
     */
    static TripleIndex forStaging(final int expectedTerms) {
        final TripleIndex index = new TripleIndex(Order.SPO);
        if (expectedTerms > 0) {
            index.stagingTerms = new StagingTerms(expectedTerms);
        }
        return index;
    }

    /**
     * Creates an empty permutation or copies the source permutation containers.
     *
     * @param source source index or container; it must remain unchanged during copying or traversal
     */
    private TripleIndex(final TripleIndex source) {
        order = source.order;
        roots = new DenseTermTable<>((DenseTermTable<Branch>) source.roots, Branch::new);
        size = source.size;
        if (source.stagingTerms != null) {
            stagingTerms = new StagingTerms(source.stagingTerms);
        }
    }

    /**
     * Adds a statement or keyed value if it is absent.
     *
     * @param s       captured statement to index
     * @param context canonical graph identifier; null denotes the default graph
     * @return true when an absent entry was added, false when it was already present
     */
    boolean add(final Statement s, final Resource context) {
        if (order != Order.SPO) {
            return add(s, new TermKey(s.getSubject()), new TermKey(s.getPredicate()), new TermKey(s.getObject()));
        }
        if (stagingTerms == null) {
            stagingTerms = new StagingTerms();
            // Seed stored keys before deriving omitted singleton keys: cross-role
            // aliases must reuse the existing subject/predicate/object key objects.
            roots.forEach((first, branch) -> stagingTerms.putIfAbsent(first, first));
            roots.values()
                    .forEach(branch -> branch.forEach((second, entry) -> stagingTerms.putIfAbsent(second, second)));
            roots.values().forEach(branch -> branch.forEach((second, entry) -> {
                if (entry instanceof Leaf leaf) {
                    leaf.forEach((third, statement) -> stagingTerms.putIfAbsent(third, third));
                }
            }));
            roots.values().forEach(branch -> branch.forEach((second, entry) -> {
                if (!(entry instanceof Leaf)) {
                    stagedKey(order.third((Statement) entry));
                }
            }));
        }
        final TermKey subject = stagedKey(s.getSubject());
        final TermKey predicate = stagedKey(s.getPredicate());
        final TermKey object = stagedKey(s.getObject());
        return add(StatementCapture.sharedStatement(s, subject, predicate, object, context), subject, predicate,
                object);
    }

    /**
     * Interns a captured RDF value in the batch-private dictionary.
     *
     * @param value non-null RDF value or entry value
     * @return the staging dictionary-owned canonical key
     */
    private TermKey stagedKey(final Value value) {
        return stagingTerms.intern(value);
    }

    /**
     * Reuses a canonical key from staging or role roots, creating one only when it is absent.
     *
     * @param value non-null RDF value or entry value
     * @return an existing canonical key, or a newly captured immutable key
     */
    private TermKey canonicalKey(final Value value) {
        final int hash = TermKey.structuralHash(value);
        TermKey key = stagingTerms == null ? null : stagingTerms.find(value, hash);
        if (key == null) {
            key = existingKey(value, hash);
        }
        return key == null ? new TermKey(value, hash) : key;
    }

    /**
     * Looks up a canonical root key using an RDF value and its structural hash.
     *
     * @param value non-null RDF value or entry value
     * @param hash  cached structural hash of the captured RDF value
     * @return the canonical root key, or null if absent
     */
    TermKey existingKey(final Value value, final int hash) {
        final Branch branch = ((DenseTermTable<Branch>) roots).get(value, hash);
        return branch == null ? null : branch.key;
    }

    /** Releases the batch-only dictionary before this index becomes visible to readers. */
    void finishStaging() {
        stagingTerms = null;
    }

    /**
     * Adds a statement or keyed value if it is absent.
     *
     * @param s         captured statement to index
     * @param subject   canonical subject key, or null for a wildcard search
     * @param predicate canonical predicate key; null denotes a wildcard for search operations
     * @param object    canonical object key, or null for a wildcard search
     * @return true when an absent entry was added, false when it was already present
     */
    boolean add(final Statement s, final TermKey subject, final TermKey predicate, final TermKey object) {
        return add(s, subject, predicate, object, this);
    }

    /**
     * Adds a statement or keyed value if it is absent.
     *
     * @param s         captured statement to index
     * @param subject   canonical subject key, or null for a wildcard search
     * @param predicate canonical predicate key; null denotes a wildcard for search operations
     * @param object    canonical object key, or null for a wildcard search
     * @param thirdKeys permutation providing canonical keys for omitted singleton values
     * @return true when an absent entry was added, false when it was already present
     */
    boolean add(final Statement s, final TermKey subject, final TermKey predicate, final TermKey object,
            final TripleIndex thirdKeys) {
        final TermKey first = first(subject, predicate, object);
        final TermKey second = second(subject, predicate, object);
        final TermKey third = third(subject, predicate, object);
        final Branch branch = roots.computeIfAbsent(first, Branch::new);
        final Object entry = branch.get(second);
        if (entry == null) {
            branch.add(second, s);
        } else if (entry instanceof Leaf leaf) {
            if (!leaf.add(third, s)) {
                return false;
            }
        } else {
            final Statement singleton = (Statement) entry;
            if (third.matches(order.third(singleton))) {
                return false;
            }
            final Leaf leaf = new Leaf();
            leaf.add(thirdKeys.canonicalKey(order.third(singleton)), singleton);
            leaf.add(third, s);
            branch.replace(second, leaf);
        }
        size++;
        return true;
    }

    /**
     * Selects the first component of this permutation.
     *
     * @param subject   canonical subject key, or null for a wildcard search
     * @param predicate canonical predicate key; null denotes a wildcard for search operations
     * @param object    canonical object key, or null for a wildcard search
     * @return the first permutation component, or null for a wildcard
     */
    private TermKey first(final TermKey subject, final TermKey predicate, final TermKey object) {
        return switch (order) {
        case SPO -> subject;
        case POS -> predicate;
        case OSP -> object;
        };
    }

    /**
     * Selects the second component of this permutation.
     *
     * @param subject   canonical subject key, or null for a wildcard search
     * @param predicate canonical predicate key; null denotes a wildcard for search operations
     * @param object    canonical object key, or null for a wildcard search
     * @return the second permutation component, or null for a wildcard
     */
    private TermKey second(final TermKey subject, final TermKey predicate, final TermKey object) {
        return switch (order) {
        case SPO -> predicate;
        case POS -> object;
        case OSP -> subject;
        };
    }

    /**
     * Selects the third component of this permutation.
     *
     * @param subject   canonical subject key, or null for a wildcard search
     * @param predicate canonical predicate key; null denotes a wildcard for search operations
     * @param object    canonical object key, or null for a wildcard search
     * @return the third permutation component, or null for a wildcard
     */
    private TermKey third(final TermKey subject, final TermKey predicate, final TermKey object) {
        return switch (order) {
        case SPO -> object;
        case POS -> subject;
        case OSP -> predicate;
        };
    }

    /**
     * Removes matching content while preserving the order of survivors.
     *
     * @param subject   canonical subject key, or null for a wildcard search
     * @param predicate canonical predicate key; null denotes a wildcard for search operations
     * @param object    canonical object key, or null for a wildcard search
     * @return true when an entry was removed, false when it was absent
     */
    boolean remove(final TermKey subject, final TermKey predicate, final TermKey object) {
        final TermKey first = first(subject, predicate, object);
        final TermKey second = second(subject, predicate, object);
        final Branch branch = roots.get(first);
        if (branch == null) {
            return false;
        }
        final Object entry = branch.get(second);
        if (entry == null) {
            return false;
        }
        final TermKey third = third(subject, predicate, object);
        if (entry instanceof Leaf leaf) {
            if (!leaf.remove(third)) {
                return false;
            }
            if (leaf.size() == 1) {
                branch.replace(second, leaf.singletonValue());
            }
        } else {
            if (!third.matches(order.third((Statement) entry))) {
                return false;
            }
            branch.remove(second);
        }
        size--;
        if (branch.isEmpty()) {
            roots.remove(first);
        }
        return true;
    }

    /**
     * Broad pattern removal: stable filtering, with one rebuild per affected dense table.
     *
     * @param subject   canonical subject key, or null for a wildcard search
     * @param predicate canonical predicate key; null denotes a wildcard for search operations
     * @param object    canonical object key, or null for a wildcard search
     * @return number of matching statements removed
     */

    long removeMatches(final TermKey subject, final TermKey predicate, final TermKey object) {
        final TermKey first = first(subject, predicate, object);
        final TermKey second = second(subject, predicate, object);
        final TermKey third = third(subject, predicate, object);
        final long[] removed = { 0 };
        if (first != null) {
            final Branch branch = roots.get(first);
            if (branch != null) {
                removed[0] = removeMatches(branch, second, third);
                if (branch.isEmpty()) {
                    roots.remove(first);
                }
            }
        } else {
            ((DenseTermTable<Branch>) roots).removeIf(branch -> {
                removed[0] += removeMatches(branch, second, third);
                return branch.isEmpty();
            });
        }
        size -= removed[0];
        return removed[0];
    }

    /**
     * Removes selected index content with stable filtering and unchanged compaction rules.
     *
     * @param branch owned or retained branch appropriate to this operation
     * @param second second-component key; null selects all branch values
     * @param third  third-component key; null selects all leaf values
     * @return number of matching statements removed
     */
    private long removeMatches(final Branch branch, final TermKey second, final TermKey third) {
        if (second != null) {
            return removeSelectedEntry(branch, second, third);
        }
        final long[] removed = { 0 };
        branch.removeIf(entry -> {
            if (third == null) {
                removed[0] += entryCount(entry, null);
                return true;
            }
            if (entry instanceof Leaf leaf) {
                if (leaf.remove(third)) {
                    removed[0]++;
                }
                return leaf.isEmpty();
            }
            if (!third.matches(order.third((Statement) entry))) {
                return false;
            }
            removed[0]++;
            return true;
        });
        // Demote after filtering: replacing values is safe in this stable traversal.
        if (third != null) {
            branch.forEach((key, entry) -> {
                if (entry instanceof Leaf leaf && leaf.size() == 1) {
                    branch.replace(key, leaf.singletonValue());
                }
            });
        }
        return removed[0];
    }

    /**
     * Removes matching content under one selected second-component key.
     *
     * @param branch owned or retained branch appropriate to this operation
     * @param second second-component key; null selects all branch values
     * @param third  third-component key; null selects all leaf values
     * @return number of matching statements removed under the selected key
     */
    private long removeSelectedEntry(final Branch branch, final TermKey second, final TermKey third) {
        final Object entry = branch.get(second);
        if (entry == null) {
            return 0;
        }
        if (third == null) {
            final long removed = entryCount(entry, null);
            branch.remove(second);
            return removed;
        }
        if (entry instanceof Leaf leaf) {
            if (!leaf.remove(third)) {
                return 0;
            }
            if (leaf.isEmpty()) {
                branch.remove(second);
            } else if (leaf.size() == 1) {
                branch.replace(second, leaf.singletonValue());
            }
            return 1;
        }
        if (!third.matches(order.third((Statement) entry))) {
            return 0;
        }
        branch.remove(second);
        return 1;
    }

    /**
     * Returns the number of live entries.
     *
     * @return live entry count
     */
    long size() {
        return size;
    }

    /**
     * Returns a materialized, ordered collection of the immutable first-component values.
     *
     * @return immutable first-component values in encounter order
     */
    Collection<Value> firstKeys() {
        return roots.keySet().stream().map(TermKey::value).toList();
    }

    /**
     * Stops as soon as visitor returns true; visits only matching index leaves.
     *
     * @param pattern captured search pattern
     * @param visitor non-mutating callback; true requests an early stop for predicate visitors
     * @return true when a matching value caused the visitor to request an early stop
     */

    boolean visit(final StatementPattern pattern, final Predicate<Statement> visitor) {
        final Value second = order.second(pattern);
        final Value third = order.third(pattern);

        final TermKey secondKey = second == null ? null : new TermKey(second);
        final TermKey thirdKey = third == null ? null : new TermKey(third);
        for (var branch : selected(roots, order.first(pattern))) {
            if (branch.visitMatching(secondKey,
                    entry -> entry instanceof Leaf leaf ? leaf.visitMatching(thirdKey, visitor)
                            : (thirdKey == null || thirdKey.matches(order.third((Statement) entry)))
                                    && visitor.test((Statement) entry))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Search keys are prepared by the caller once, then reused across graph partitions.
     *
     * @param subject   canonical subject key, or null for a wildcard search
     * @param predicate canonical predicate key; null denotes a wildcard for search operations
     * @param object    canonical object key, or null for a wildcard search
     * @param visitor   non-mutating callback; true requests an early stop for predicate visitors
     * @return true when a matching value caused the visitor to request an early stop
     */

    boolean visit(final TermKey subject, final TermKey predicate, final TermKey object,
            final Predicate<Statement> visitor) {
        final TermKey secondKey = second(subject, predicate, object);
        final TermKey thirdKey = third(subject, predicate, object);

        for (var branch : selected(roots, first(subject, predicate, object))) {
            if (branch.visitMatching(secondKey,
                    entry -> entry instanceof Leaf leaf ? leaf.visitMatching(thirdKey, visitor)
                            : (thirdKey == null || thirdKey.matches(order.third((Statement) entry)))
                                    && visitor.test((Statement) entry))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Selects the complete ordered value view or the value of one key.
     *
     * @param map unchanged ordered map to select from
     * @param key search or insertion key; null denotes a wildcard only for pattern traversal
     * @param <T> selected entry value type
     * @return the complete ordered value view or an immutable singleton selection
     */
    private static <T> Collection<T> selected(final Map<TermKey, T> map, final Value key) {
        if (key == null) {
            return map.values();
        }
        final T value = map.get(new TermKey(key));
        return value == null ? List.of() : List.of(value);
    }

    /**
     * Selects the complete ordered value view or the value of one key.
     *
     * @param map unchanged ordered map to select from
     * @param key search or insertion key; null denotes a wildcard only for pattern traversal
     * @param <T> selected entry value type
     * @return the complete ordered value view or an immutable singleton selection
     */
    private static <T> Collection<T> selected(final Map<TermKey, T> map, final TermKey key) {
        if (key == null) {
            return map.values();
        }
        final T value = map.get(key);
        return value == null ? List.of() : List.of(value);
    }

    /**
     * Copies all mutable index containers, sharing only immutable keys and statements.
     *
     * @return an independently owned copy of the mutable containers
     */
    TripleIndex copy() {
        return new TripleIndex(this);
    }

    /**
     * Caller retains the partition for the entire cursor lifetime. No per-prefix iterators.
     *
     * @param subject   canonical subject key, or null for a wildcard search
     * @param predicate canonical predicate key; null denotes a wildcard for search operations
     * @param object    canonical object key, or null for a wildcard search
     * @return a single-consumer positional cursor
     */
    TripleIndexCursor cursor(final TermKey subject, final TermKey predicate, final TermKey object) {
        return new TripleIndexCursor(this, first(subject, predicate, object), second(subject, predicate, object),
                third(subject, predicate, object));
    }

    /**
     * Counts matching prefixes without visiting every statement in a dense leaf.
     *
     * @param pattern captured search pattern
     * @return matching statement count
     */

    long count(final StatementPattern pattern) {
        if (order.first(pattern) == null && order.second(pattern) == null && order.third(pattern) == null) {
            return size;
        }
        long count = 0;
        final Value third = order.third(pattern);
        final TermKey thirdKey = third == null ? null : new TermKey(third);
        for (var branch : selected(roots, order.first(pattern))) {
            final Value second = order.second(pattern);
            if (second == null) {
                count += branch.sum(entry -> entryCount(entry, thirdKey));
            } else {
                final Object entry = branch.get(new TermKey(second));
                if (entry != null) {
                    count += entryCount(entry, thirdKey);
                }
            }
        }
        return count;
    }

    /**
     * Estimates predicate statistics (count, distinct subjects, distinct objects)
     * using the POS index permutation where the predicate is the root key.
     *
     * @param predicate the predicate IRI
     * @param maxSample maximum number of branch entries to sample for distinct subjects
     * @return predicate statistics or null if not found or not in POS order
     */
    PredicateStatistics estimatePredicateStatistics(final IRI predicate, final int maxSample) {
        return PredicateStatisticsEstimator.estimate(this, predicate, maxSample);
    }

    /**
     * Counts matching statements in the captured or session-visible state.
     *
     * @param subject   canonical subject key, or null for a wildcard search
     * @param predicate canonical predicate key; null denotes a wildcard for search operations
     * @param object    canonical object key, or null for a wildcard search
     * @return matching statement count
     */
    long count(final TermKey subject, final TermKey predicate, final TermKey object) {
        if (subject == null && predicate == null && object == null) {
            return size;
        }
        long count = 0;
        final TermKey secondKey = second(subject, predicate, object);
        final TermKey thirdKey = third(subject, predicate, object);

        for (var branch : selected(roots, first(subject, predicate, object))) {
            if (secondKey == null) {
                count += branch.sum(entry -> entryCount(entry, thirdKey));
            } else {
                final Object entry = branch.get(secondKey);
                if (entry != null) {
                    count += entryCount(entry, thirdKey);
                }
            }
        }
        return count;
    }

    /**
     * Counts matches in a singleton statement or a multivalued leaf.
     *
     * @param entry immutable singleton statement or compact multivalued leaf
     * @param third third-component key; null selects all leaf values
     * @return matching leaf cardinality
     */
    private long entryCount(final Object entry, final TermKey third) {
        if (entry instanceof Leaf leaf) {
            if (third == null) {
                return leaf.size();
            }
            return leaf.get(third) == null ? 0 : 1;
        }
        return third == null || third.matches(order.third((Statement) entry)) ? 1 : 0;
    }

    /** Root keys are also addressable as values for cross-permutation canonicalization. */
    static final class Branch extends CompactTermMap<Object> {
        /**
         * Canonical first-component key retained by this root branch.
         */
        private final TermKey key;

        /**
         * Creates a root branch retaining its canonical key or copies its nested containers.
         *
         * @param key search or insertion key; null denotes a wildcard only for pattern traversal
         */
        Branch(final TermKey key) {
            this.key = key;
        }

        /**
         * Creates a root branch retaining its canonical key or copies its nested containers.
         *
         * @param source source index or container; it must remain unchanged during copying or traversal
         */
        Branch(final Branch source) {
            super(source, entry -> entry instanceof Leaf leaf ? leaf.copy() : entry);
            key = source.key;
        }
    }

    /** Multi-statement leaf only; singleton statements live directly in their branch. */
    static final class Leaf extends CompactTermMap<Statement> {
        /**
         * Creates an empty multivalued leaf or copies the source leaf container.
         */
        Leaf() {
        }

        /**
         * Creates an empty multivalued leaf or copies the source leaf container.
         *
         * @param source source index or container; it must remain unchanged during copying or traversal
         */
        Leaf(final Leaf source) {
            super(source);
        }

        /**
         * Copies owned mutable containers while sharing immutable keys and statements.
         *
         * @return an independently owned copy of the mutable containers
         */
        private Leaf copy() {
            return new Leaf(this);
        }
    }

    /**
     * Receives immutable SPO statements and their canonical term keys during index construction.
     */
    @FunctionalInterface
    interface EntryVisitor {
        /**
         * Receives one immutable statement and its canonical SPO keys.
         *
         * @param subject   canonical subject key, or null for a wildcard search
         * @param predicate canonical predicate key; null denotes a wildcard for search operations
         * @param object    canonical object key, or null for a wildcard search
         * @param statement statement whose RDF identities have been captured
         */
        void accept(final TermKey subject, final TermKey predicate, final TermKey object, final Statement statement);
    }

    /**
     * Visits canonical SPO entries while constructing secondary permutations.
     *
     * @param visitor non-mutating callback; true requests an early stop for predicate visitors
     * @throws IllegalStateException if the required representation, ownership or lifecycle state is not satisfied
     */
    void visitEntries(final EntryVisitor visitor) {
        if (order != Order.SPO) {
            throw new IllegalStateException("Bulk source must be SPO");
        }
        final EntryTraversal traversal = new EntryTraversal(this, visitor);
        roots.forEach(traversal::root);
    }

    /** One call-local traversal, avoiding a capturing callback per prefix/leaf. */
    private static final class EntryTraversal {
        /**
         * SPO index supplying statements and canonical keys.
         */
        private final TripleIndex source;
        /**
         * Batch-local canonical-entry callback.
         */
        private final EntryVisitor visitor;
        /**
         * Callback reused across predicate branches.
         */
        private final BiConsumer<TermKey, Object> branches = this::branch;
        /**
         * Callback reused across multivalued object leaves.
         */
        private final BiConsumer<TermKey, Statement> leaves = this::leaf;
        /**
         * Canonical traversal subject or current batch subject, according to the enclosing traversal.
         */
        private TermKey subject;
        /**
         * Canonical traversal predicate or current batch predicate, according to the enclosing traversal.
         */
        private TermKey predicate;

        /**
         * Creates call-local SPO traversal callbacks for the source and visitor.
         *
         * @param source  source index or container; it must remain unchanged during copying or traversal
         * @param visitor non-mutating callback; true requests an early stop for predicate visitors
         */
        private EntryTraversal(final TripleIndex source, final EntryVisitor visitor) {
            this.source = source;
            this.visitor = visitor;
        }

        /**
         * Traverses one SPO root using call-local subject and predicate positions.
         *
         * @param subject canonical subject key, or null for a wildcard search
         * @param branch  owned or retained branch appropriate to this operation
         */
        private void root(final TermKey subject, final Branch branch) {
            this.subject = subject;
            branch.forEach(branches);
        }

        /**
         * Traverses one predicate branch, recovering an omitted singleton object key if necessary.
         *
         * @param predicate canonical predicate key; null denotes a wildcard for search operations
         * @param entry     immutable singleton statement or compact multivalued leaf
         */
        private void branch(final TermKey predicate, final Object entry) {
            this.predicate = predicate;
            if (entry instanceof Leaf leaf) {
                leaf.forEach(leaves);
            } else {
                final Statement singleton = (Statement) entry;
                visitor.accept(subject, predicate, source.canonicalKey(singleton.getObject()), singleton);
            }
        }

        /**
         * Forwards one object-keyed statement with the current subject and predicate keys.
         *
         * @param object    canonical object key, or null for a wildcard search
         * @param statement statement whose RDF identities have been captured
         */
        private void leaf(final TermKey object, final Statement statement) {
            visitor.accept(subject, predicate, object, statement);
        }
    }
}
