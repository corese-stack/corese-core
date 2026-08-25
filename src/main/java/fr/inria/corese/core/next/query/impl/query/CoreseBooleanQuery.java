package fr.inria.corese.core.next.query.impl.query;

import fr.inria.corese.core.next.query.api.BooleanQuery;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.execution.NextSparqlPipelineExecutor;

import java.util.Objects;

/**
 * Prepared SPARQL ASK query.
 *
 * <p>Delegates evaluation to {@link NextSparqlPipelineExecutor}.</p>
 */
public final class CoreseBooleanQuery extends AbstractCoreseQuery<Boolean> implements BooleanQuery {

    private final NextSparqlPipelineExecutor executor;

    public CoreseBooleanQuery(
            String queryString,
            NextSparqlPipelineExecutor executor,
            Runnable executionGuard) {
        super(queryString, executionGuard);
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public Boolean evaluate() throws QueryEvaluationException {
        checkExecutable();
        return executor.evaluateBoolean(getQueryString(), getBindings(), getDataset(), timeoutMillis());
    }
}
