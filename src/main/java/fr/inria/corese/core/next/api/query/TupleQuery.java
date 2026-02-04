package fr.inria.corese.core.next.api.query;

import fr.inria.corese.core.next.api.query.exception.QueryEvaluationException;
import fr.inria.corese.core.next.api.query.result.TupleQueryResult;

/**
 * Compiled SPARQL query corresponding to a SELECT query
 */
public interface TupleQuery extends Query<TupleQueryResult> {
    /**
     * Evaluates this SELECT query and returns the result as a sequence of variable bindings.
     *
     * @return a {@link TupleQueryResult} containing the query solutions
     * @throws QueryEvaluationException if an error occurs during query evaluation
     */
    @Override
    TupleQueryResult evaluate() throws QueryEvaluationException;


}