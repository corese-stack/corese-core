package fr.inria.corese.core.next.query.api;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;

/**
 * Prepared SPARQL ASK query.
 */
public interface BooleanQuery extends Query<Boolean> {
    /**
     * Evaluates this ASK query and returns whether any solutions exist.
     *
     * @return {@code true} if the query has at least one solution, {@code false} otherwise
     * @throws QueryEvaluationException if an error occurs during query evaluation
     */
    @Override
    Boolean evaluate() throws QueryEvaluationException;
}
