package fr.inria.corese.core.next.kgram.api.query;

import fr.inria.corese.core.next.kgram.api.core.Node;
import fr.inria.corese.core.next.kgram.core.Mapping;
import fr.inria.corese.core.next.kgram.core.Mappings;
import fr.inria.corese.core.next.kgram.core.Query;
import fr.inria.corese.core.next.kgram.core.SparqlException;

/**
 * @author corby
 */
public interface SPARQLEngine {

    Mappings eval(Query q, Mapping m, Producer p) throws SparqlException;

    Mappings eval(Node gNode, Query q, Mapping m, Producer p) throws SparqlException;

    // within a lock where query processing has ended and where we can process another query
    boolean isSynchronized();

    void setSynchronized(boolean b);

}
