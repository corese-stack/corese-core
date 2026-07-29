package fr.inria.corese.core.next.query.kgram.sorter.core;

import fr.inria.corese.core.next.query.kgram.api.query.Producer;

/**
 * Interface for estimating the cost of nodes and edges in QPGraph
 *
 * @author Fuqi Song, WImmics Inria I3S
 */
public interface IEstimate {

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
     * @param parameters parameters for different implementations
     */
    void estimate(QPGraph plein, Producer producer, Object parameters);
    
}
