package fr.inria.corese.core.next.query.api.exception;

/**
 * Thrown when a query evaluation exceeds its configured timeout limit.
 */
public class QueryTimeoutException extends QueryEvaluationException {

    /**
     * Constructs a QueryTimeoutException with the timeout value that was exceeded.
     *
     * @param timeoutMillis the timeout limit that was exceeded, in milliseconds
     */
    public QueryTimeoutException(long timeoutMillis) {
        super(String.format("Query exceeded timeout of %d milliseconds", timeoutMillis));
    }


}