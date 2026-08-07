package fr.inria.corese.core.next.query.impl.query;

import fr.inria.corese.core.next.query.api.GraphQuery;
import fr.inria.corese.core.next.query.api.Query;
import fr.inria.corese.core.next.query.api.QueryLanguage;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.execution.NextSparqlPipelineExecutor;

/**
 * Prepared SPARQL CONSTRUCT or DESCRIBE query.
 *
 * <p>Delegates evaluation to {@link NextSparqlPipelineExecutor}. Initial bindings,
 * dataset override, and timeout are propagated into the KGRAM evaluation layer
 * on each {@link #evaluate()} call. The CONSTRUCT template is applied to each
 * WHERE-clause result mapping to materialise the output statements.</p>
 */
public final class CoreseGraphQuery extends AbstractCoreseOperation implements GraphQuery {

    private final NextSparqlPipelineExecutor executor;
    private long timeoutMillis = 0;

    public CoreseGraphQuery(String queryString, QueryLanguage language, NextSparqlPipelineExecutor executor) {
        super(queryString, language);
        this.executor = executor;
    }

    @Override
    public Query<GraphQueryResult> setTimeout(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    @Override
    public QueryType getQueryType() {
        return QueryType.GRAPH;
    }

    @Override
    public GraphQueryResult evaluate() throws QueryEvaluationException {
        return executor.evaluateGraph(getQueryString(), getBindings(), getDataset(), effectiveTimeoutMillis());
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
