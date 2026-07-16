package fr.inria.corese.core.next.query.api.exception;

/**
 * Thrown when a syntactically valid query uses a feature not implemented by the current next pipeline.
 */
@SuppressWarnings("java:S110")
public class UnsupportedQueryFeatureException extends QueryException {

    /**
     * Constructs an UnsupportedQueryFeatureException with a detail message.
     *
     * @param message the detail message explaining which query feature is not supported
     */
    public UnsupportedQueryFeatureException(String message) {
        super(message);
    }

    /**
     * Constructs an UnsupportedQueryFeatureException with a detail message and cause.
     *
     * @param message the detail message explaining which query feature is not supported
     * @param cause   the underlying cause that exposed the unsupported feature
     */
    public UnsupportedQueryFeatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
