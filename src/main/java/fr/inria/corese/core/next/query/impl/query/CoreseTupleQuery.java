package fr.inria.corese.core.next.query.impl.query;

import fr.inria.corese.core.next.query.api.TupleQuery;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.execution.NextSparqlPipelineExecutor;

import java.util.Objects;

/**
 * Prepared SPARQL SELECT query.
 *
 * <p>Delegates evaluation to {@link NextSparqlPipelineExecutor}. Initial bindings,
 * dataset override, and timeout are propagated into the KGRAM evaluation layer
 * on each {@link #evaluate()} call.</p>
 */
public final class CoreseTupleQuery extends AbstractCoreseQuery<TupleQueryResult> implements TupleQuery {

    private final NextSparqlPipelineExecutor executor;

    public CoreseTupleQuery(
            String queryString,
            NextSparqlPipelineExecutor executor,
            Runnable executionGuard) {
        super(queryString, executionGuard);
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public TupleQueryResult evaluate() throws QueryEvaluationException {
        checkExecutable();
        return executor.evaluateTuple(getQueryString(), getBindings(), getDataset(), timeoutMillis());
    }
}
