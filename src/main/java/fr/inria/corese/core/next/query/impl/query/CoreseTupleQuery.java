package fr.inria.corese.core.next.query.impl.query;

import fr.inria.corese.core.next.query.api.Query;
import fr.inria.corese.core.next.query.api.QueryLanguage;
import fr.inria.corese.core.next.query.api.TupleQuery;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.execution.NextSparqlPipelineExecutor;

/**
 * Prepared SPARQL SELECT query.
 *
 * <p>Delegates evaluation to {@link NextSparqlPipelineExecutor}. Initial bindings,
 * dataset override, and timeout are propagated into the KGRAM evaluation layer
 * on each {@link #evaluate()} call.</p>
 */
public final class CoreseTupleQuery extends AbstractCoreseOperation implements TupleQuery {

    private final NextSparqlPipelineExecutor executor;
    private long timeoutMillis = 0;

    public CoreseTupleQuery(String queryString, QueryLanguage language, NextSparqlPipelineExecutor executor) {
        super(queryString, language);
        this.executor = executor;
    }

    @Override
    public Query<TupleQueryResult> setTimeout(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    @Override
    public QueryType getQueryType() {
        return QueryType.TUPLE;
    }

    @Override
    public TupleQueryResult evaluate() throws QueryEvaluationException {
        return executor.evaluateTuple(getQueryString(), getBindings(), getDataset(), effectiveTimeoutMillis());
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
