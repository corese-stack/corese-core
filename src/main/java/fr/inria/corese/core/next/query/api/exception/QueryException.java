package fr.inria.corese.core.next.query.api.exception;


import fr.inria.corese.core.next.data.api.base.exception.CoreseException;

/**
 * Base exception for all query-related errors in the Corese query API.
 */
public class QueryException extends CoreseException {

    /**
     * Wraps the QueryException around the given Throwable
     *
     * @param e a throwable
     */
    public QueryException(Throwable e) {
        super(e);
    }

    /**
     * Constructs a new QueryException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public QueryException(String message) {
        super(message);
    }

    /**
     * Constructs a new QueryException with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause   the underlying cause of this exception (may be {@code null})
     */
    public QueryException(String message, Throwable cause) {
        super(message, cause);
    }
}