package fr.inria.corese.core.next.query.kgram.api.core;

import java.util.List;

/**
 * @author Olivier Corby, Wimmics, INRIA 2019
 */
public interface DatatypeValueFactory {

    Node nodeList(List<Node> list);

    Node nodeValue(int n);

}
