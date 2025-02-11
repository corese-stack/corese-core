package fr.inria.corese.core.kgram.api.core;

import fr.inria.corese.core.kgram.path.Path;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.api.IDatatype.NodeKind;

import static fr.inria.corese.core.kgram.api.core.PointerType.NODE;

/**
 * Interface of Node provided by graph implementation
 * and also by KGRAM query Node
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public interface Node extends Pointerable, Comparable {
    String INITKEY = "";

    int DEPTH = 0;
    int LENGTH = 1;
    int REGEX = 2;
    int OBJECT = 3;


    int PSIZE = 4;

    int STATUS = 4;

    default NodeKind getNodeKind() {
        return getValue().getNodeKind();
    }

    @Override
    default PointerType pointerType() {
        return NODE;
    }

    /**
     * Query nodes have an index computed by KGRAM
     *
     * @return
     */
    int getIndex();

    /**
     * Query nodes have an index computed by KGRAM
     *
     * @return
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

    @Override
    default int compareTo(Object obj) {
        if(obj instanceof Node) {
            return compareTo((Node) obj);
        }
        return -1;
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

    @Override
    TripleStore getTripleStore();

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

    default boolean isTripleNode() {
        return false;
    }
}
