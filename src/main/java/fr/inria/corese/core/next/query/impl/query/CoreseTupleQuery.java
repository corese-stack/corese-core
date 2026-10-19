package fr.inria.corese.core.next.query.impl.query;

import fr.inria.corese.core.next.query.api.TupleQuery;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.execution.SparqlQueryExecutor;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;

/**
 * Prepared SPARQL SELECT query.
 *
 * <p>Delegates evaluation to {@link SparqlQueryExecutor}. Initial bindings,
 * dataset override, and timeout are propagated into the KGRAM evaluation layer
 * on each {@link #evaluate()} call.</p>
 */
public final class CoreseTupleQuery extends AbstractCoreseQuery<TupleQueryResult> implements TupleQuery {

    public CoreseTupleQuery(
            String queryString,
            SparqlQueryExecutor executor,
            Runnable executionGuard) {
        this(queryString, null, executor, executionGuard);
    }

    /** Retains immutable compilation separately from execution state for repeated evaluations. */
    public CoreseTupleQuery(
            String queryString,
            SelectQueryAst parsedQuery,
            SparqlQueryExecutor executor,
            Runnable executionGuard) {
        super(queryString, executionGuard, executor, parsedQuery);
    }

    @Override
    protected TupleQueryResult evaluate(SparqlQueryExecutor execution) {
        return execution.evaluateTuple(preparedPlan(), getBindings(), getDataset(), timeoutMillis());
    }
}
