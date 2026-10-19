package fr.inria.corese.core.next.query.impl.engine.planning;

import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

/**
 * Assigns selectivity costs to the nodes and edges of a planning graph.
 *
 * @author Fuqi Song, WImmics Inria I3S
 */
public interface SelectivityEstimator {

    double MAX_COST = 1.0;
    //approximate minimum value, but not equal to 0
    double MIN_COST_0 = 1.0 / Double.MAX_VALUE;
    double NA_COST = -1;

    /**
     * Estimate and assign the selectvity (or other criteria) for each node in
     * the given BP graph
     *
     * @param plein graph
     * @param producer producer
     */
    void estimate(QueryPlanGraph plein, Producer producer);

}
