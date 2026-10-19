package fr.inria.corese.core.next.query.impl.query;

import fr.inria.corese.core.next.query.api.GraphQuery;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.execution.SparqlQueryExecutor;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;

/**
 * Prepared SPARQL CONSTRUCT or DESCRIBE query.
 *
 * <p>Delegates evaluation to {@link SparqlQueryExecutor}. Initial bindings,
 * dataset override, and timeout are propagated into the KGRAM evaluation layer
 * on each {@link #evaluate()} call. The CONSTRUCT template is applied to each
 * WHERE-clause result mapping to materialise the output statements.</p>
 */
public final class CoreseGraphQuery extends AbstractCoreseQuery<GraphQueryResult> implements GraphQuery {

    public CoreseGraphQuery(
            String queryString,
            SparqlQueryExecutor executor,
            Runnable executionGuard) {
        this(queryString, null, executor, executionGuard);
    }

    /** Retains immutable compilation separately from execution state for repeated evaluations. */
    public CoreseGraphQuery(
            String queryString,
            QueryAst parsedQuery,
            SparqlQueryExecutor executor,
            Runnable executionGuard) {
        super(queryString, executionGuard, executor, parsedQuery);
    }

    @Override
    protected GraphQueryResult evaluate(SparqlQueryExecutor execution) {
        return execution.evaluateGraph(preparedPlan(), getBindings(), getDataset(), timeoutMillis());
    }
}
