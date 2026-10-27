package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;

/**
 * One graph and its three permutations (SPO, POS, OSP).
 *
 * <p>
 * References count State owners, not statements or rows.
 * </p>
 */
final class GraphPartition {

    /**
     * Number of live State owners, guarded by the database monitor.
     */
    int references = 1;

    /**
     * Stable graph registry position.
     */
    final long ordinal;

    /**
     * Canonical graph identifier; null denotes the default graph.
     */
    final Resource context;

    /**
     * Subject/predicate/object permutation of this graph.
     */
    final TripleIndex spo;

    /**
     * Predicate/object/subject permutation of this graph.
     */
    final TripleIndex pos;

    /**
     * Object/subject/predicate permutation of this graph.
     */
    final TripleIndex osp;

    /**
     * Creates a graph partition owning the supplied permutations or empty indexes.
     *
     * @param context canonical graph identifier; null denotes the default graph
     * @param ordinal stable graph registry ordinal
     */
    GraphPartition(final Resource context, final long ordinal) {
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
    GraphPartition(final Resource context, final long ordinal, final TripleIndex spo, final TripleIndex pos,
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
    static GraphPartition build(final TripleIndex spo, final Resource context, final long ordinal) {
        final TripleIndex.SecondaryIndexes secondary = TripleIndex.buildSecondaryIndexes(spo);
        final GraphPartition partition = new GraphPartition(context, ordinal, spo, secondary.pos(), secondary.osp());
        spo.finishStaging();
        return partition;
    }

    /**
     * Copies owned mutable containers while sharing immutable keys and statements.
     *
     * @return an independently owned copy of the mutable containers
     */
    GraphPartition copy() {
        return new GraphPartition(context, ordinal, spo.copy(), pos.copy(), osp.copy());
    }

    /**
     * Adds a statement or keyed value if it is absent.
     *
     * @param s captured statement to index
     * @return true when an absent entry was added, false when it was already present
     */
    boolean add(Statement s) {
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
    boolean remove(final Statement s) {
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
    void removeMatches(final StatementPattern pattern) {
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
    TripleIndex index(final StatementPattern p) {
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
    boolean contains(final Statement statement) {
        return spo.count(
                StatementPattern.of(statement.getSubject(), statement.getPredicate(), statement.getObject())) != 0;
    }
}
