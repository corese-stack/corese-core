package fr.inria.corese.core.next.query.kgram.api.core;

import fr.inria.corese.core.next.query.kgram.path.Path;
import fr.inria.corese.core.next.query.kgram.sparql.api.IDatatype;


/**
 * Interface of Node provided by graph implementation
 * and also by KGRAM query Node
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public interface Node extends Pointerable<Object>, Comparable<Node> {
    String INITKEY = "";

    int REGEX = 2;
    int OBJECT = 3;


    default fr.inria.corese.core.next.query.kgram.sparql.api.IDatatype.NodeKind getNodeKind() {
        return getValue().getNodeKind();
    }


    /**
     * Query nodes have an index computed by KGRAM
     *
     */
    int getIndex();

    /**
     * Query nodes have an index computed by KGRAM
     *
     */
    void setIndex(int n);

    String getKey();

    void setKey(String str);


    /**
     * sameTerm
     */
    boolean same(Node n);

    // Node match for Graph match
    boolean match(Node n);

    int compare(Node node);

    default int compareTo(Node node) {
        return getDatatypeValue().compareTo(node.getDatatypeValue());
    }


    String getLabel();

    boolean isVariable();

    boolean isConstant();

    boolean isBlank();

    boolean isFuture();

    default boolean isMatchNodeList() {
        return false;
    }

    default boolean isMatchCardinality() {
        return false;
    }

    // the target value for Matcher and Evaluator
    // for KGRAM query it returns IDatatype
    IDatatype getValue();

    IDatatype getDatatypeValue();

    default void setDatatypeValue(IDatatype dt) {
    }

    Node getGraph();

    @Override
    Node getNode();

    Object getNodeObject();

    void setObject(Object o);

    Path getPath();

    // tagged as triple reference
    default boolean isTriple() {
        return getDatatypeValue().isTriple();
    }

    // triple reference with edge inside
    default boolean isTripleWithEdge() {
        return isTriple() && getEdge() != null;
    }

    default void setEdge(Edge e) {
        getDatatypeValue().setEdge(e);
    }

}
