package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;

import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * Evaluation-thread bounded ORDER BY collector. The worst retained solution is
 * at the heap root. Arrival ordinals preserve the stable full sort's tie order,
 * including ties crossing OFFSET or the selection boundary. Projection and
 * DISTINCT must already have been applied by the input cursor.
 *
 * Only retained slots allocate ranking wrappers: replacement reuses the evicted
 * slot. Candidate Mapping objects are still produced by the ordinary evaluator.
 */
final class TopKSelector {
    private final Mappings ordering;
    private final PriorityQueue<RankedMapping> heap;
    private final int capacity;
    private final int offset;
    private final boolean needsInput;
    private long arrival;

    /**
     * Creates a selector without eagerly reserving a potentially large LIMIT.
     *
     * @param query completed eligible ordered SELECT with finite LIMIT plus OFFSET
     * @param eval evaluator supplying the official Corese ordering comparator
     */
    TopKSelector(Query query, Eval eval) {
        capacity = Math.addExact(query.getLimit(), query.getOffset());
        offset = query.getOffset();
        needsInput = query.getLimit() != 0;
        ordering = Mappings.create(query);
        ordering.setEval(eval);
        heap = new PriorityQueue<>(Math.clamp(capacity, 1, 16), this::worstFirst);
    }

    /** @return whether a nonzero LIMIT requires evaluating the input */
    boolean needsInput() {
        return needsInput;
    }

    /**
     * Retains a candidate only if it improves the bounded selection. Equal later
     * candidates cannot displace an earlier one at the selection boundary.
     *
     * @param row projected and deduplicated solution
     */
    void offer(Mapping row) {
        long ordinal = arrival++;
        if (heap.size() < capacity) {
            heap.add(new RankedMapping(row, ordinal));
        } else if (capacity > 0 && ordering.compare(row, heap.element().row) < 0) {
            RankedMapping slot = heap.remove();
            slot.row = row;
            slot.arrival = ordinal;
            heap.add(slot);
        }
    }

    private int bestFirst(RankedMapping left, RankedMapping right) {
        int comparison = ordering.compare(left.row, right.row);
        return comparison == 0 ? Long.compare(left.arrival, right.arrival) : comparison;
    }

    @SuppressWarnings("java:S2234") // Reverse the stable best-first comparison so the worst retained row heads the heap.
    private int worstFirst(RankedMapping left, RankedMapping right) {
        return bestFirst(right, left);
    }

    /**
     * Sorts at most K winners and applies OFFSET once. No evaluator, heap slots
     * or discarded candidates escape with the returned final mappings.
     *
     * @return selected mappings in stable Corese order
     */
    Mappings finish() {
        try {
            var winners = new ArrayList<>(heap);
            winners.sort(this::bestFirst);
            for (int i = offset; i < winners.size(); i++) ordering.add(winners.get(i).row);
            return ordering;
        } finally {
            heap.clear();
            ordering.setEval(null);
        }
    }

    /** Mutable heap slot, reused after eviction rather than allocated for every candidate. */
    private static final class RankedMapping {
        private Mapping row;
        private long arrival;

        private RankedMapping(Mapping row, long arrival) {
            this.row = row;
            this.arrival = arrival;
        }
    }
}
