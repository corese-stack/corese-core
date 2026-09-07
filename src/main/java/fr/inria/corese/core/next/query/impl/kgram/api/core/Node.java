package fr.inria.corese.core.next.query.impl.kgram.api.core;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.impl.kgram.path.Path;


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
        return compare(node);
    }

    String getLabel();

    boolean isVariable();

    boolean isConstant();

    boolean isBlank();

    default boolean isMatchNodeList() {
        return false;
    }

    default boolean isMatchCardinality() {
        return false;
    }

    // the target value for Matcher and Evaluator
    DatatypeValue getValue();

    DatatypeValue getDatatypeValue();

    default void setDatatypeValue(DatatypeValue dt) {
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

}
