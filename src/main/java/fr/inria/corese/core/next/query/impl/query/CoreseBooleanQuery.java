package fr.inria.corese.core.next.query.impl.query;

import fr.inria.corese.core.next.query.api.BooleanQuery;
import fr.inria.corese.core.next.query.impl.sparql.execution.SparqlQueryExecutor;
import fr.inria.corese.core.next.query.impl.sparql.ast.AskQueryAst;

/**
 * Prepared SPARQL ASK query.
 *
 * <p>Delegates evaluation to {@link SparqlQueryExecutor}.</p>
 */
public final class CoreseBooleanQuery extends AbstractCoreseQuery<Boolean> implements BooleanQuery {

    public CoreseBooleanQuery(
            String queryString,
            SparqlQueryExecutor executor,
            Runnable executionGuard) {
        this(queryString, null, executor, executionGuard);
    }

    /** Retains immutable compilation separately from execution state for repeated evaluations. */
    public CoreseBooleanQuery(
            String queryString,
            AskQueryAst parsedQuery,
            SparqlQueryExecutor executor,
            Runnable executionGuard) {
        super(queryString, executionGuard, executor, parsedQuery);
    }

    @Override
    protected Boolean evaluate(SparqlQueryExecutor execution) {
        return execution.evaluateBoolean(preparedPlan(), getBindings(), getDataset(), timeoutMillis());
    }
}
