package fr.inria.corese.core.next.query.api.exception;

/**
 * Thrown when a query is syntactically valid but violates a static validation rule.
 */
@SuppressWarnings("java:S110")
public class QueryValidationException extends QueryException {

    /**
     * Constructs a QueryValidationException with a detail message.
     *
     * @param message the detail message explaining why the query is invalid
     */
    public QueryValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a QueryValidationException with a detail message and cause.
     *
     * @param message the detail message explaining why the query is invalid
     * @param cause   the underlying cause of the validation failure
     */
    public QueryValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
