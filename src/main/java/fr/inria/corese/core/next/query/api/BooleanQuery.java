package fr.inria.corese.core.next.query.api;

import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;

/**
 * Compiled SPARQL query corresponding to an ASK query.
 */
public interface BooleanQuery extends Query<Boolean> {
    /**
     * Evaluates this ASK query and returns whether any solutions exist.
     *
     * @return {@code true} if the query has at least one solution, {@code false} otherwise
     * @throws QueryEvaluationException if an error occurs during query evaluation
     */
    @Override
    Boolean evaluate() throws QueryEvaluationException;

    @Override
    BooleanQuery setBinding(String name, Value value);

    @Override
    BooleanQuery removeBinding(String name);

    @Override
    BooleanQuery clearBindings();

    @Override
    BooleanQuery setDataset(Dataset dataset);

    @Override
    BooleanQuery setIncludeInferred(boolean includeInferred);

    @Override
    BooleanQuery setMaxExecutionTime(int maxExecutionTimeSeconds);

    @Override
    BooleanQuery setTimeout(long timeoutMillis);
}