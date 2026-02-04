package fr.inria.corese.core.next.api.query.exception;

/**
 * Thrown when an operation on a {@link fr.inria.corese.core.next.api.query.repository.Repository}
 * or {@link fr.inria.corese.core.next.api.query.repository.RepositoryConnection} fails.
 */
public class RepositoryException extends QueryException {

    /**
     * Constructs a RepositoryException with a detail message.
     *
     * @param message the detail message explaining why the repository operation failed
     */
    public RepositoryException(String message) {
        super(message);
    }

    /**
     * Constructs a RepositoryException with a detail message and cause.
     *
     * @param message the detail message explaining why the repository operation failed
     * @param cause   the underlying cause of the repository failure (e.g., IOException,
     *                SQLException, ConfigurationException, etc.)
     */
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}