package fr.inria.corese.core.next.data.api.query;

import fr.inria.corese.core.next.data.api.query.exception.QueryEvaluationException;
import fr.inria.corese.core.next.data.api.query.result.GraphQueryResult;

public interface GraphQuery extends Query<GraphQueryResult> {

    @Override
    GraphQueryResult evaluate() throws QueryEvaluationException;
}
