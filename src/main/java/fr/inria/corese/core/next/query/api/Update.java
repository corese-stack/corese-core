package fr.inria.corese.core.next.query.api;

import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;

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
    @Override
    Update setBinding(String name, Value value);

    @Override
    Update removeBinding(String name);

    @Override
    Update clearBindings();

    @Override
    Update setDataset(Dataset dataset);

    @Override
    Update setIncludeInferred(boolean includeInferred);

    @Override
    Update setMaxExecutionTime(int maxExecutionTimeSeconds);
}
