package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.query.impl.engine.pattern.Query;

/**
 * Strategy interface for direct group aggregation without general expression evaluation.
 */
@FunctionalInterface
public interface GroupAggregator {

    /**
     * Attempts to reduce input mappings directly according to the query's group and aggregate projections.
     *
     * @param query query whose group and aggregate projections apply
     * @param mappings input mappings to reduce
     * @return {@code true} if aggregation succeeded, {@code false} if ordinary evaluation is required
     */
    boolean tryAggregate(Query query, Mappings mappings);
}
