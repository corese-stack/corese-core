package fr.inria.corese.core.next.query.impl.engine.spi;

import fr.inria.corese.core.next.query.impl.engine.model.Node;

/**
 * Interface to KGRAM elementary result (a mapping)
 *
 * @author corby
 */
public interface Result {

    Node getNode(String name);

    Node getNode(Node node);

}
