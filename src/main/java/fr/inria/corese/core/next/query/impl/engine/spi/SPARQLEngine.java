package fr.inria.corese.core.next.query.impl.engine.spi;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.eval.SparqlException;

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
