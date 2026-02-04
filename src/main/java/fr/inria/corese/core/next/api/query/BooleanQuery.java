package fr.inria.corese.core.next.api.query;

import fr.inria.corese.core.next.api.query.exception.QueryEvaluationException;

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