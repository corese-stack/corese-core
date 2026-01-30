package fr.inria.corese.core.next.api.query;

import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.query.result.TupleQueryResult;

/**
 * Compiled SPARQL query corresponding to a SELECT query
 */
public interface TupleQuery extends Query<TupleQueryResult> {
    @Override
    TupleQueryResult evaluate();


}