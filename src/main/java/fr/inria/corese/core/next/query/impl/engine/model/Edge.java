package fr.inria.corese.core.next.query.impl.engine.model;

import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;

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
    default void setEdgeIndex(int n) {
        // Most immutable edge implementations do not expose a mutable index.
    }

    Node getGraph();


    @Override
    Edge getEdge();

    default Object getProvenance() {
        return null;
    }

    default void setProvenance(Object obj) {
        // Provenance is an optional capability of concrete edge implementations.
    }

    default boolean isMatchArity() {
        return false;
    }

    // nested rdf star triple <<s p o>>
    default boolean isNested() {
        return false;
    }
    default void setNested(boolean b) {
        // RDF-star nesting is an optional capability of concrete edge implementations.
    }

    default DatatypeValue getGraphValue() {
        Node node = getGraph();
        if (node == null) {
            return null;
        }
        return node.getDatatypeValue();
    }

    default DatatypeValue getSubjectValue() {
        return getNode(0).getDatatypeValue();
    }


    default DatatypeValue getPredicateValue() {
        if (getProperty() == null) {
            return null;
        }
        return getProperty().getDatatypeValue();
    }

    default DatatypeValue getObjectValue() {
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
