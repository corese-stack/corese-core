package fr.inria.corese.core.next.query.impl.query;

import fr.inria.corese.core.next.query.api.BooleanQuery;
import fr.inria.corese.core.next.query.api.Query;
import fr.inria.corese.core.next.query.api.QueryLanguage;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.execution.NextSparqlPipelineExecutor;

/**
 * Prepared SPARQL ASK query.
 *
 * <p>Delegates evaluation to {@link NextSparqlPipelineExecutor}.</p>
 */
public final class CoreseBooleanQuery extends AbstractCoreseOperation implements BooleanQuery {

    private final NextSparqlPipelineExecutor executor;
    private long timeoutMillis = 0;

    public CoreseBooleanQuery(String queryString, QueryLanguage language, NextSparqlPipelineExecutor executor) {
        super(queryString, language);
        this.executor = executor;
    }

    @Override
    public Query<Boolean> setTimeout(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    @Override
    public QueryType getQueryType() {
        return QueryType.BOOLEAN;
    }

    @Override
    public Boolean evaluate() throws QueryEvaluationException {
        return executor.evaluateBoolean(getQueryString(), getBindings(), getDataset(), effectiveTimeoutMillis());
    }

    private long effectiveTimeoutMillis() {
        long fromQuery = this.timeoutMillis;
        long fromOperation = (long) getMaxExecutionTime() * 1000L;
        if (fromQuery > 0 && fromOperation > 0) {
            return Math.min(fromQuery, fromOperation);
        }
        return fromQuery > 0 ? fromQuery : fromOperation;
    }
}
