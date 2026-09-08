package fr.inria.corese.core.next.query.impl.engine.sorter;

import fr.inria.corese.core.next.query.impl.engine.model.Node;

import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;

import java.util.List;

/**
 * Interface for sorting and rewriting the QPG nodes
 *
 * @author Fuqi Song, WImmics Inria I3S
 */
public interface ISort {

    /**
     * Sort the QPG node
     *
     * @param unsorted graph
     * @return List of sorted QPG Node
     */
    List<QPGNode> sort(QPGraph unsorted);

    /**
     * Rewrite the SPARQL exp according to give order of nodes
     */
    void rewrite(Exp exp, List<QPGNode> nodes, int start);
}
