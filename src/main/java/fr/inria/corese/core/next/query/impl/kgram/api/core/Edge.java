package fr.inria.corese.core.next.query.impl.kgram.api.core;

import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * Interface for Producer iterator that encapsulate Edge or Node with its Graph
 * Node
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public interface Edge extends Pointerable<Object> {


    // nb nodes to consider in sparql query processing
    default int nbNode() {
        return 2;
    }

    /**
     * nodes that are vertex of the graph use case: metadata node is not a graph
     * vertex
     */
    default int nbGraphNode() {
        return nbNode();
    }

    Node getNode(int i);


    default Node getEdgeNode() {
        return getProperty();
    }

    default Node getEdgeVariable() {
        return null;
    }

    // edge variable or edge node
    Node getProperty();


    /**
     * Is node returned by getNode()
     *
     */
    default boolean contains(Node node) {
        return false;
    }


    String getEdgeLabel();


    default int getEdgeIndex() {
        return -1;
    }
    @SuppressWarnings("unused")
    default void setEdgeIndex(int n) {
    }

    Node getGraph();


    @Override
    Edge getEdge();

    default Object getProvenance() {
        return null;
    }

    default void setProvenance(Object obj) {
    }

    default boolean isMatchArity() {
        return false;
    }

    // nested rdf star triple <<s p o>>
    default boolean isNested() {
        return false;
    }
    @SuppressWarnings("unused")
    default void setNested(boolean b) {
    }

    default IDatatype getGraphValue() {
        Node node = getGraph();
        if (node == null) {
            return null;
        }
        return node.getDatatypeValue();
    }

    default IDatatype getSubjectValue() {
        return getNode(0).getDatatypeValue();
    }


    default IDatatype getPredicateValue() {
        if (getProperty() == null) {
            return null;
        }
        return getProperty().getDatatypeValue();
    }

    default IDatatype getObjectValue() {
        return getNode(1).getDatatypeValue();
    }

    default boolean isTripleNode() {
        return false;
    }

    default boolean sameTermWithoutGraph(Edge e) {
        return getSubjectValue().sameTerm(e.getSubjectValue())
                && getPredicateValue().sameTerm(e.getPredicateValue())
                && getObjectValue().sameTerm(e.getObjectValue());
    }

    default boolean equalsWithoutGraph(Edge e) {
        return getObjectValue().equals(e.getObjectValue())
                && getSubjectValue().equals(e.getSubjectValue())
                && getPredicateValue().equals(e.getPredicateValue());
    }

}
