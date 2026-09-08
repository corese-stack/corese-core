package fr.inria.corese.core.next.query.impl.engine.model;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;

/**
 * @author corby
 */
public interface TripleStore {

    Node getNode(int n);

    DatatypeValue set(DatatypeValue key, DatatypeValue value);

    int size();

}
