package fr.inria.corese.core.next.storage.impl.memory;

/**
 * Builds secondary POS and OSP index permutations from a staged SPO triple index.
 */
final class SecondaryIndexBuilder {

    private SecondaryIndexBuilder() {
        // Utility class
    }

    /**
     * Completed secondary permutations sharing the SPO statements and canonical keys.
     *
     * @param pos completed POS permutation
     * @param osp completed OSP permutation
     */
    record Result(TripleIndex pos, TripleIndex osp) {
    }

    /**
     * Reconstruct omitted singleton keys once while building both secondary permutations.
     *
     * @param source source index or container; it must remain unchanged during copying or traversal
     * @return the completed POS and OSP permutations
     */
    static Result build(final TripleIndex source) {
        final TripleIndex pos = new TripleIndex(TripleIndex.Order.POS);
        final TripleIndex osp = new TripleIndex(TripleIndex.Order.OSP);
        source.visitEntries((subject, predicate, object, statement) -> {
            pos.add(statement, subject, predicate, object, source);
            osp.add(statement, subject, predicate, object, source);
        });
        return new Result(pos, osp);
    }
}
