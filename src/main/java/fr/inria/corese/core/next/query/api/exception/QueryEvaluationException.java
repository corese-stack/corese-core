package fr.inria.corese.core.next.query.api.exception;

/**
 * Thrown when a syntactically valid query fails during evaluation (execution).
 */
public class QueryEvaluationException extends QueryException {

    /**
     * Constructs a QueryEvaluationException with a detail message.
     *
     * @param message the detail message explaining why the query evaluation failed
     */
    public QueryEvaluationException(String message) {
        super(message);
    }

    /**
     * Constructs a QueryEvaluationException with a detail message and cause.
     *
     * @param message the detail message explaining why the query evaluation failed
     * @param cause   the underlying cause of the evaluation failure (e.g., IOException,
     *                SQLException, or other runtime exceptions)
     */
    public QueryEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}