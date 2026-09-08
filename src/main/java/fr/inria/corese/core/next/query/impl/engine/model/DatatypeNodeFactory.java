package fr.inria.corese.core.next.query.impl.engine.model;

import java.util.List;

/**
 * @author Olivier Corby, Wimmics, INRIA 2019
 */
public interface DatatypeNodeFactory {

    Node nodeList(List<Node> list);

    Node nodeValue(int n);

}
