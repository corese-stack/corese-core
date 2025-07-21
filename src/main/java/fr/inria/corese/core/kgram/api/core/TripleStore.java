package fr.inria.corese.core.kgram.api.core;

import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * @author corby
 */
public interface TripleStore {

    Node getNode(int n);

    IDatatype set(IDatatype key, IDatatype value);

    int size();

}
