package fr.inria.corese.core.next.query.impl.query;

import fr.inria.corese.core.next.query.api.GraphQuery;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.execution.NextSparqlPipelineExecutor;

import java.util.Objects;

/**
 * Prepared SPARQL CONSTRUCT or DESCRIBE query.
 *
 * <p>Delegates evaluation to {@link NextSparqlPipelineExecutor}. Initial bindings,
 * dataset override, and timeout are propagated into the KGRAM evaluation layer
 * on each {@link #evaluate()} call. The CONSTRUCT template is applied to each
 * WHERE-clause result mapping to materialise the output statements.</p>
 */
public final class CoreseGraphQuery extends AbstractCoreseQuery<GraphQueryResult> implements GraphQuery {

    private final NextSparqlPipelineExecutor executor;

    public CoreseGraphQuery(
            String queryString,
            NextSparqlPipelineExecutor executor,
            Runnable executionGuard) {
        super(queryString, executionGuard);
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public GraphQueryResult evaluate() throws QueryEvaluationException {
        checkExecutable();
        return executor.evaluateGraph(getQueryString(), getBindings(), getDataset(), timeoutMillis());
    }
}
