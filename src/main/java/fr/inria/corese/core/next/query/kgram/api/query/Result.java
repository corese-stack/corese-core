package fr.inria.corese.core.next.query.kgram.api.query;

import fr.inria.corese.core.next.query.kgram.api.core.Node;

/**
 * Interface to KGRAM elementary result (a mapping)
 *
 * @author corby
 */
public interface Result {

    Node getNode(String name);

    Node getNode(Node node);

}
