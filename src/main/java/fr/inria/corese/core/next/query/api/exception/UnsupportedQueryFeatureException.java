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

}
