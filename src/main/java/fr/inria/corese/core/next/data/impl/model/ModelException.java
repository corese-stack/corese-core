package fr.inria.corese.core.next.data.impl.model;

/**
 * Exception thrown when a Model operation fails.
 */
public class ModelException extends RuntimeException {

    /**
     * The operation that failed.
     */
    private final ModelOperation operation;

    /**
     * Creates a new ModelException.
     *
     * @param operation the operation that failed
     * @param message   the error message
     */
    public ModelException(ModelOperation operation, String message) {
        super(formatMessage(operation, message));
        this.operation = operation;
    }

    /**
     * Creates a new ModelException with a cause.
     *
     * @param operation the operation that failed
     * @param message   the error message
     * @param cause     the underlying cause
     */
    public ModelException(ModelOperation operation, String message, Throwable cause) {
        super(formatMessage(operation, message), cause);
        this.operation = operation;
    }

    /**
     * Returns the operation that failed.
     *
     * @return the failed operation
     */
    public ModelOperation getOperation() {
        return operation;
    }

    /**
     * Formats the exception message.
     *
     * @param operation the operation
     * @param message   the message
     * @return formatted message
     */
    private static String formatMessage(ModelOperation operation, String message) {
        return String.format("Model operation '%s' failed: %s", operation.name(), message);
    }

    /**
     * Enumeration of Model operations that can fail.
     */
    public enum ModelOperation {
        /**
         * Add statement(s)
         */
        ADD,

        /**
         * Remove statement(s)
         */
        REMOVE,

        /**
         * Clear statement(s)
         */
        CLEAR,

        /**
         * Check if statement exists
         */
        CONTAINS,

        /**
         * Query/retrieve statements
         */
        QUERY,

        /**
         * Get subjects
         */
        GET_SUBJECTS,

        /**
         * Get predicates
         */
        GET_PREDICATES,

        /**
         * Get objects
         */
        GET_OBJECTS,

        /**
         * Get contexts
         */
        GET_CONTEXTS,

        /**
         * Count statements
         */
        SIZE,

        /**
         * Create iterator
         */
        ITERATE
    }
}
