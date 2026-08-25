package fr.inria.corese.core.next.query.api;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;

/**
 * Prepared SPARQL CONSTRUCT or DESCRIBE query.
 */
public interface GraphQuery extends Query<GraphQueryResult> {

    @Override
    GraphQueryResult evaluate() throws QueryEvaluationException;
}
