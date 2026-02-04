package fr.inria.corese.core.next.api.query;

import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.query.exception.QueryEvaluationException;

/**
 * Represents a prepared SPARQL UPDATE operation.
 */
public interface Update extends Operation {
    /**
     * Executes this SPARQL UPDATE operation.
     *
     * @throws QueryEvaluationException if an error occurs during update execution
     */
    void execute() throws QueryEvaluationException;

    /**
     * Binds a variable in this update operation.
     *
     * @param name  the variable name (without '?')
     * @param value the RDF value to bind
     * @return this Update instance for method chaining
     */
    Update setBinding(String name, Value value);

}
