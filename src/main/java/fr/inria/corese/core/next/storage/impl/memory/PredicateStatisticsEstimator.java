package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.storage.api.model.PredicateStatistics;

import java.util.HashSet;
import java.util.Set;

/**
 * Estimates predicate cardinality and distinct term distribution from POS index branches.
 */
final class PredicateStatisticsEstimator {

    private PredicateStatisticsEstimator() {
        // Utility class
    }

    /**
     * Estimates predicate statistics from the POS permutation.
     *
     * @param index     POS triple index
     * @param predicate the predicate IRI
     * @param maxSample maximum number of subjects to sample for distinct-subject estimation
     * @return predicate statistics or null if not found or not in POS order
     */
    static PredicateStatistics estimate(final TripleIndex index, final IRI predicate, final int maxSample) {
        if (index.order != TripleIndex.Order.POS || predicate == null) {
            return null;
        }
        final TermKey pKey = new TermKey(predicate);
        final TripleIndex.Branch branch = index.roots.get(pKey);
        if (branch == null) {
            return null;
        }
        final long distinctObjects = branch.size();
        if (distinctObjects == 0) {
            return new PredicateStatistics(predicate, 0, 0, 0);
        }

        final PredicateSampleAccumulator accumulator = new PredicateSampleAccumulator(maxSample);
        branch.forEach((objKey, entry) -> accumulator.accumulate(entry));

        final long distinctSubjects = accumulator.estimateDistinctSubjects(distinctObjects);
        return new PredicateStatistics(predicate, accumulator.totalCount, distinctSubjects, distinctObjects);
    }

    private static final class PredicateSampleAccumulator {
        private final Set<TermKey> subjects = new HashSet<>();
        private final int maxSample;
        private long totalCount = 0;
        private int sampled = 0;

        PredicateSampleAccumulator(final int maxSample) {
            this.maxSample = maxSample;
        }

        void accumulate(final Object entry) {
            if (entry instanceof TripleIndex.Leaf leaf) {
                accumulateLeaf(leaf);
            } else if (entry instanceof Statement statement) {
                accumulateStatement(statement);
            }
        }

        private void accumulateLeaf(final TripleIndex.Leaf leaf) {
            totalCount += leaf.size();
            if (shouldSample()) {
                leaf.visitMatching(null, statement -> {
                    subjects.add(new TermKey(statement.getSubject()));
                    sampled++;
                    return !shouldSample(); // true = stop when subject limit reached
                });
            }
        }

        private void accumulateStatement(final Statement statement) {
            totalCount++;
            if (shouldSample()) {
                subjects.add(new TermKey(statement.getSubject()));
                sampled++;
            }
        }

        private boolean shouldSample() {
            return maxSample <= 0 || sampled < maxSample;
        }

        long estimateDistinctSubjects(final long distinctObjects) {
            if (maxSample <= 0 || sampled < maxSample) {
                // Processed fewer subjects than the limit: sampled everything exactly.
                return subjects.size();
            }
            // Hit the subject limit — extrapolate from the sample.
            final double ratio = (double) totalCount / Math.max(1, sampled);
            final long estimated = Math.round(subjects.size() * ratio);
            return Math.min(totalCount, Math.max(subjects.size(), estimated));
        }
    }
}
