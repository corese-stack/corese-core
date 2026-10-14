package fr.inria.corese.core.next.query.impl.query;

import fr.inria.corese.core.next.query.api.BooleanQuery;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.execution.NextSparqlPipelineExecutor;
import fr.inria.corese.core.next.query.impl.sparql.ast.AskQueryAst;

import java.util.Objects;

/**
 * Prepared SPARQL ASK query.
 *
 * <p>Delegates evaluation to {@link NextSparqlPipelineExecutor}.</p>
 */
public final class CoreseBooleanQuery extends AbstractCoreseQuery<Boolean> implements BooleanQuery {

    private final NextSparqlPipelineExecutor executor;
    private final AskQueryAst parsedQuery;

    public CoreseBooleanQuery(
            String queryString,
            NextSparqlPipelineExecutor executor,
            Runnable executionGuard) {
        this(queryString, null, executor, executionGuard);
    }

    /** Reuses the preparation AST while constructing fresh runtime state for each evaluation. */
    public CoreseBooleanQuery(
            String queryString,
            AskQueryAst parsedQuery,
            NextSparqlPipelineExecutor executor,
            Runnable executionGuard) {
        super(queryString, executionGuard);
        this.executor = Objects.requireNonNull(executor, "executor");
        this.parsedQuery = parsedQuery;
    }

    @Override
    public Boolean evaluate() throws QueryEvaluationException {
        checkExecutable();
        if (parsedQuery == null) {
            return executor.evaluateBoolean(getQueryString(), getBindings(), getDataset(), timeoutMillis());
        }
        return executor.evaluateBoolean(parsedQuery, getBindings(), getDataset(), timeoutMillis());
    }
}
