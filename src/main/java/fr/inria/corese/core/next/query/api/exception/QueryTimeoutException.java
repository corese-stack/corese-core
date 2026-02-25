package fr.inria.corese.core.next.query.api.exception;

/**
 * Thrown when a query evaluation exceeds its configured timeout limit.
 */
public class QueryTimeoutException extends QueryEvaluationException {

    private final long timeoutMillis;

    /**
     * Constructs a QueryTimeoutException with the timeout value that was exceeded.
     *
     * @param timeoutMillis the timeout limit that was exceeded, in milliseconds
     */
    public QueryTimeoutException(long timeoutMillis) {
        super(String.format("Query exceeded timeout of %d milliseconds", timeoutMillis));
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Constructs a QueryTimeoutException with a custom message and timeout value.
     *
     * @param message       the detail message explaining the timeout
     * @param timeoutMillis the timeout limit that was exceeded, in milliseconds
     */
    public QueryTimeoutException(String message, long timeoutMillis) {
        super(message);
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Constructs a QueryTimeoutException with a message, timeout value, and cause.
     *
     * @param message       the detail message explaining the timeout
     * @param timeoutMillis the timeout limit that was exceeded, in milliseconds
     * @param cause         the underlying cause of the timeout (e.g., SocketTimeoutException)
     */
    public QueryTimeoutException(String message, long timeoutMillis, Throwable cause) {
        super(message, cause);
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Returns the timeout value that was exceeded.
     *
     * @return the timeout limit in milliseconds that was exceeded
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * Returns the timeout value in seconds.
     *
     * @return the timeout limit in seconds (truncated from milliseconds)
     */
    public long getTimeoutSeconds() {
        return timeoutMillis / 1000;
    }
}