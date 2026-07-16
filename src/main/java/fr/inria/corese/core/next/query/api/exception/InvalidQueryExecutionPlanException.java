package fr.inria.corese.core.next.query.api.exception;

/**
 * Thrown when the compiled query execution plan is inconsistent with the
 * contract of a Corese-next execution component.
 */
public class InvalidQueryExecutionPlanException extends QueryException {

    /**
     * Constructs an InvalidQueryExecutionPlanException with a detail message.
     *
     * @param message the detail message explaining the invalid execution plan
     */
    public InvalidQueryExecutionPlanException(String message) {
        super(message);
    }

    /**
     * Constructs an InvalidQueryExecutionPlanException with a detail message and cause.
     *
     * @param message the detail message explaining the invalid execution plan
     * @param cause   the underlying cause that exposed the invalid plan
     */
    public InvalidQueryExecutionPlanException(String message, Throwable cause) {
        super(message, cause);
    }
}
