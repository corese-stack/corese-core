package fr.inria.corese.core.next.api.query;

import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.dataset.Dataset;
import fr.inria.corese.core.next.api.result.BindingSet;

/**
 * A generic SPARQL operation (query or update) on a Corese repository.
 *
 * It allows predefined variable bindings, a custom dataset, and execution options
 * such as inference and timeout.
 */
public interface Operation {

    /**
     * Binds the specified variable to the supplied value.
     * Any previous value for this variable is overwritten.
     *
     * @param name  the variable name (without '?')
     * @param value the value to bind
     */
    void setBinding(String name, Value value);

    /**
     * Removes a previously set binding on the supplied variable.
     * Calling this method with an unbound variable has no effect.
     *
     * @param name the variable name (without '?')
     */
    void removeBinding(String name);

    /**
     * Removes all previously set bindings for this operation.
     */
    void clearBindings();

    /**
     * Retrieves the bindings that have been set on this operation.
     *
     * @return a (possibly empty) set of variable bindings
     */
    BindingSet getBindings();

    /**
     * Specifies the dataset (FROM / FROM NAMED) against which to execute this operation,
     * overriding any dataset specified in the query text itself.
     *
     * @param dataset the dataset to use, or {@code null} to clear
     */
    void setDataset(Dataset dataset);

    /**
     * Gets the dataset that has been set on this operation, if any.
     *
     * @return the dataset, or {@code null} if none has been explicitly set
     */
    Dataset getDataset();

    /**
     * Determines whether inferred statements (if available) should be taken into account
     * during evaluation. The default is {@code true}.
     *
     * @param includeInferred whether inferred statements should be included
     */
    void setIncludeInferred(boolean includeInferred);

    /**
     * @return {@code true} if inferred statements are included in evaluation, {@code false} otherwise
     */
    boolean getIncludeInferred();

    /**
     * Specifies the maximum time that this operation is allowed to run.
     * A non-positive value means 'no limit'.
     *
     * @param maxExecutionTimeSeconds maximum execution time in seconds
     */
    void setMaxExecutionTime(int maxExecutionTimeSeconds);

    /**
     * @return the maximum execution time in seconds, or a non-positive value if unlimited
     */
    int getMaxExecutionTime();

}
