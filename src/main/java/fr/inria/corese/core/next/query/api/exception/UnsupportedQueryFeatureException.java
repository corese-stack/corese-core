package fr.inria.corese.core.next.query.api.exception;

/**
 * Thrown when a syntactically valid query uses a feature not implemented by the current next pipeline.
 */
@SuppressWarnings("java:S110")
public class UnsupportedQueryFeatureException extends QueryException {

    public UnsupportedQueryFeatureException(String message) {
        super(message);
    }

    public UnsupportedQueryFeatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
