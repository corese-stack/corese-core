package fr.inria.corese.core.next.query.impl.kgram.api.core;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;

/**
 * @author corby
 */
public interface TripleStore {

    Node getNode(int n);

    DatatypeValue set(DatatypeValue key, DatatypeValue value);

    int size();

}
