package fr.inria.corese.core.next.query.kgram.api.core;

import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.parser.AccessRight;

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

    @SuppressWarnings("unused")
    default void setNode(int i, Node n) {
    }


    default Node getEdgeNode() {
        return getProperty();
    }

    default Node getEdgeVariable() {
        return null;
    }

    // edge variable or edge node
    Node getProperty();

    @SuppressWarnings("unused")
    default void setProperty(Node node) {
    }


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

    // manage access right
    default AccessRight.AccessRights getLevel() {
        return AccessRight.AccessRights.NONE;
    }
    @SuppressWarnings("unused")
    default Edge setLevel(AccessRight.AccessRights b) {
        return this;
    }


    // use case: internal index edge
    default boolean isInternal() {
        return nbNode() == 2 && !isTripleNode();
    }

    @Override
    Node getNode();

    Node getGraph();
    @SuppressWarnings("unused")
    default void setGraph(Node n) {
    }

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

    default boolean isAsserted() {
        return !isNested();
    }

    default void setAsserted(boolean b) {
        setNested(!b);
    }

    // edge created as nested triple expression
    // bind (<<s p o>> as ?t)
    // values ?t { <<s p o>> }
    default boolean isCreated() {
        return false;
    }
    @SuppressWarnings("unused")
    default void setCreated(boolean b) {
    }

    default Node getGraphNode() {
        return getGraph();
    }

    default Node getSubjectNode() {
        return getNode(0);
    }

    default Node getPropertyNode() {
        return getProperty();
    }

    default Node getObjectNode() {
        return getNode(1);
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

    default boolean sameTerm(Edge e) {
        return sameTermWithoutGraph(e)
                && (getGraphValue() == null || e.getGraphValue() == null
                ? getGraphValue() == e.getGraphValue()
                : getGraphValue().sameTerm(e.getGraphValue()));
    }

    default boolean sameTermWithoutGraph(Edge e) {
        return getSubjectValue().sameTerm(e.getSubjectValue())
                && getPredicateValue().sameTerm(e.getPredicateValue())
                && getObjectValue().sameTerm(e.getObjectValue());
    }

    default boolean equals(Edge e) {
        return equalsWithoutGraph(e)
                && (getGraphValue() == null || e.getGraphValue() == null
                ? getGraphValue() == e.getGraphValue()
                : getGraphValue().equals(e.getGraphValue()));
    }

    default boolean equalsWithoutGraph(Edge e) {
        return getObjectValue().equals(e.getObjectValue())
                && getSubjectValue().equals(e.getSubjectValue())
                && getPredicateValue().equals(e.getPredicateValue());
    }

}
