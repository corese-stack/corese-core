package fr.inria.corese.core.next.query.impl.query;

import fr.inria.corese.core.next.query.api.TupleQuery;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.execution.NextSparqlPipelineExecutor;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;

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
    private final SelectQueryAst parsedQuery;

    public CoreseTupleQuery(
            String queryString,
            NextSparqlPipelineExecutor executor,
            Runnable executionGuard) {
        this(queryString, null, executor, executionGuard);
    }

    /** Reuses the preparation AST while constructing fresh runtime state for each evaluation. */
    public CoreseTupleQuery(
            String queryString,
            SelectQueryAst parsedQuery,
            NextSparqlPipelineExecutor executor,
            Runnable executionGuard) {
        super(queryString, executionGuard);
        this.executor = Objects.requireNonNull(executor, "executor");
        this.parsedQuery = parsedQuery;
    }

    @Override
    public TupleQueryResult evaluate() throws QueryEvaluationException {
        checkExecutable();
        if (parsedQuery == null) {
            return executor.evaluateTuple(getQueryString(), getBindings(), getDataset(), timeoutMillis());
        }
        return executor.evaluateTuple(parsedQuery, getBindings(), getDataset(), timeoutMillis());
    }
}
