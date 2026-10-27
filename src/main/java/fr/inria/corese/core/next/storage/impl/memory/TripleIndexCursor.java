package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.model.Statement;

/**
 * Single-consumer positional cursor over an unchanged partition retained by its caller.
 */
final class TripleIndexCursor {

    private final TripleIndex index;
    private final TermKey first;
    private final TermKey second;
    private final TermKey third;
    private int rootPosition;
    private int branchPosition;
    private int leafPosition;
    private boolean selectedRoot;
    private TripleIndex.Branch branch;
    private TripleIndex.Leaf leaf;

    /**
     * Captures the index and search keys for unchanged positional traversal.
     *
     * @param index  source index containing roots and ordering
     * @param first  first-component key; null selects all roots
     * @param second second-component key; null selects all branch values
     * @param third  third-component key; null selects all leaf values
     */
    TripleIndexCursor(final TripleIndex index, final TermKey first, final TermKey second, final TermKey third) {
        this.index = index;
        this.first = first;
        this.second = second;
        this.third = third;
    }

    /**
     * Internal cursor: null marks exhaustion. The public Spliterator supplies Iterator semantics.
     *
     * @return the next statement, or null at exhaustion for the internal index cursor
     */
    Statement next() {
        for (;;) {
            if (leaf != null) {
                final Statement result = nextLeafStatement();
                if (result != null) {
                    return result;
                }
            }
            if (branch != null) {
                final Statement result = nextBranchStatement();
                if (result != null) {
                    return result;
                }
                continue;
            }
            if (!selectNextRoot()) {
                return null;
            }
            branchPosition = 0;
        }
    }

    /**
     * Reads the next match from the current leaf and drops that traversal reference on exhaustion.
     *
     * @return the next matching leaf statement, or null when the leaf is exhausted
     */
    private Statement nextLeafStatement() {
        if (third != null) {
            final Statement result = leaf.get(third);
            leaf = null;
            if (result != null) {
                return result;
            }
        } else {
            final int position = leaf.nextIndex(leafPosition);
            if (position >= 0) {
                leafPosition = position + 1;
                return leaf.valueAt(position);
            }
            leaf = null;
        }
        return null;
    }

    /**
     * Reads a branch match or selects a multivalued leaf for subsequent traversal.
     *
     * @return a matching singleton statement, or null when traversal must continue
     */
    private Statement nextBranchStatement() {
        final Object entry = nextBranchEntry();
        if (entry instanceof TripleIndex.Leaf values) {
            leaf = values;
            leafPosition = 0;
            return null;
        }
        if (entry != null && (third == null || third.matches(index.order.third((Statement) entry)))) {
            return (Statement) entry;
        }
        return null;
    }

    /**
     * Reads a selected branch value or advances its positional traversal.
     *
     * @return the selected branch entry, or null when no entry remains
     */
    private Object nextBranchEntry() {
        Object entry;
        if (second != null) {
            entry = branch.get(second);
            branch = null;
        } else {
            final int position = branch.nextIndex(branchPosition);
            if (position < 0) {
                branch = null;
                return null;
            }
            branchPosition = position + 1;
            entry = branch.valueAt(position);
        }
        return entry;
    }

    /**
     * Selects the next root once, preserving its encounter order.
     *
     * @return true when a root was selected, false at exhaustion
     */
    @SuppressWarnings("unchecked")
    private boolean selectNextRoot() {
        if (first != null) {
            if (selectedRoot) {
                return false;
            }
            selectedRoot = true;
            branch = index.roots.get(first);
            if (branch == null) {
                return false;
            }
        } else {
            final var table = (DenseTermTable<TripleIndex.Branch>) index.roots;
            final int position = table.nextIndex(rootPosition);
            if (position < 0) {
                return false;
            }
            rootPosition = position + 1;
            branch = table.valueAt(position);
        }
        return true;
    }
}
