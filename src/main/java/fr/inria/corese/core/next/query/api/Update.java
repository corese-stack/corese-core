package fr.inria.corese.core.next.query.api;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;

/**
 * Represents a prepared SPARQL UPDATE operation.
 */
public interface Update {
    /**
     * Executes this SPARQL UPDATE operation.
     *
     * @throws QueryEvaluationException if an error occurs during update execution
     */
    void execute() throws QueryEvaluationException;
}
