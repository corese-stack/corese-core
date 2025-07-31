package fr.inria.corese.core.sparql.exceptions;

/**
 * Custom exception for operations within the SQLFun class.
 * This provides a more specific type for runtime errors that occur
 * during database interactions or driver loading, making error handling
 * more explicit and easier to manage by callers.
 */
public class SQLFunException extends RuntimeException {
    /**
     * Constructs a new SQLFunException with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public SQLFunException(String message) {
        super(message);
    }

    /**
     * Constructs a new SQLFunException with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).
     * (A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public SQLFunException(String message, Throwable cause) {
        super(message, cause);
    }
}