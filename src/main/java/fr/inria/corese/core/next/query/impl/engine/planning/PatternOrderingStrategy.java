package fr.inria.corese.core.next.query.impl.engine.planning;

import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;

import java.util.List;

/**
 * Selects a traversal order and rewrites an expression group in that order.
 *
 * @author Fuqi Song, WImmics Inria I3S
 */
public interface PatternOrderingStrategy {

    /**
     * Sort the QPG node
     *
     * @param unsorted graph
     * @return List of sorted QPG Node
     */
    List<QueryPlanNode> sort(QueryPlanGraph unsorted);

    /**
     * Rewrite the SPARQL exp according to give order of nodes
     */
    void rewrite(Exp exp, List<QueryPlanNode> nodes, int start);
}
