package fr.inria.corese.core.next.query.api.exception;

/**
 * Thrown when a SPARQL query or update string contains invalid syntax.
 */
public class QuerySyntaxException extends QueryException {

    private final int line;
    private final int column;


    /**
     * Constructs a QuerySyntaxException with a detail message and position information.
     *
     * @param message the detail message explaining the syntax error
     * @param line    the line number where the error occurred (1-based), or -1 if unknown
     * @param column  the column number where the error occurred (1-based), or -1 if unknown
     */
    public QuerySyntaxException(String message, int line, int column) {
        super(formatMessage(message, line, column));
        this.line = line;
        this.column = column;
    }

    /**
     * Constructs a QuerySyntaxException with a detail message and cause.
     *
     * @param message the detail message explaining the syntax error
     * @param cause   the underlying cause of this exception (typically a parser exception)
     */
    public QuerySyntaxException(String message, Throwable cause) {
        this(message, -1, -1, cause);
    }

    /**
     * Constructs a QuerySyntaxException with a detail message, position information, and cause.
     *
     * @param message the detail message explaining the syntax error
     * @param line    the line number where the error occurred (1-based), or -1 if unknown
     * @param column  the column number where the error occurred (1-based), or -1 if unknown
     * @param cause   the underlying cause of this exception (typically a parser exception)
     */
    public QuerySyntaxException(String message, int line, int column, Throwable cause) {
        super(formatMessage(message, line, column), cause);
        this.line = line;
        this.column = column;
    }

    /**
     * Constructs a QuerySyntaxException with a detail message, position information, and cause.
     *
     * @param message the detail message explaining the syntax error
     */
    public QuerySyntaxException(String message) {
        super(message);
        this.line = -1;
        this.column = -1;
    }

    /**
     * Returns the line number where the syntax error occurred.
     *
     * @return the line number (1-based), or -1 if position information is not available
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the column number where the syntax error occurred.
     *
     * @return the column number (1-based), or -1 if position information is not available
     */
    public int getColumn() {
        return column;
    }

    /**
     * Formats the error message to include line and column information when available.
     *
     * @param message the base error message
     * @param line    the line number (1-based), or -1 if unknown
     * @param column  the column number (1-based), or -1 if unknown
     * @return the formatted error message
     */
    private static String formatMessage(String message, int line, int column) {
        if (line >= 1 && column >= 1) {
            return String.format("Syntax error at line %d, column %d: %s", line, column, message);
        } else if (line >= 1) {
            return String.format("Syntax error at line %d: %s", line, message);
        } else {
            return message;
        }
    }
}