package fr.inria.corese.core.next.query.api;

import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;

/**
 * Compiled SPARQL query corresponding to a CONSTRUCT or DESCRIBE query.
 */
public interface GraphQuery extends Query<GraphQueryResult> {

    @Override
    GraphQueryResult evaluate() throws QueryEvaluationException;

    @Override
    GraphQuery setBinding(String name, Value value);

    @Override
    GraphQuery removeBinding(String name);

    @Override
    GraphQuery clearBindings();

    @Override
    GraphQuery setDataset(Dataset dataset);

    @Override
    GraphQuery setIncludeInferred(boolean includeInferred);

    @Override
    GraphQuery setMaxExecutionTime(int maxExecutionTimeSeconds);

    @Override
    GraphQuery setTimeout(long timeoutMillis);
}
