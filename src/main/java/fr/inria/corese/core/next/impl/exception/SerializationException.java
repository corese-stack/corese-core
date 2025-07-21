package fr.inria.corese.core.next.impl.exception;

import fr.inria.corese.core.next.api.base.exception.CoreseException;

/**
 * Exception thrown during RDF serialization or deserialization failures.
 * This exception can carry format-specific details (e.g., NTriples, JSON-LD,
 * XML, etc.),
 * as well as information about the location of the error within the data
 * stream.
 */
public class SerializationException extends CoreseException {
    private final String formatName;
    private final int lineNumber;
    private final int columnNumber;

    public SerializationException(String message, String formatName) {
        this(message, formatName, -1, -1, null);
    }

    /**
     * Constructs a new {@code SerializationException} with the specified detail
     * message,
     * format name, and cause. Line and column numbers are set to -1 (unknown).
     *
     * @param message    the detail message (which is saved for later retrieval by
     *                   the {@link #getMessage()} method).
     * @param formatName the name of the RDF format being processed when the error
     *                   occurred.
     *                   Use "unknown" if the format is not applicable or cannot be
     *                   determined.
     * @param cause      the cause (which is saved for later retrieval by the
     *                   {@link #getCause()} method).
     *                   (A {@code null} value is permitted, and indicates that the
     *                   cause is nonexistent or unknown.)
     */
    public SerializationException(String message, String formatName, Throwable cause) {
        this(message, formatName, -1, -1, cause);
    }

    /**
     * Constructs a new {@code SerializationException} with the specified detail
     * message,
     * format name, line number, column number, and cause.
     *
     * @param message      the detail message.
     * @param formatName   the name of the RDF format being processed.
     * @param lineNumber   the line number where the error occurred, or -1 if
     *                     unknown.
     * @param columnNumber the column number where the error occurred, or -1 if
     *                     unknown.
     * @param cause        the cause of the exception.
     */
    public SerializationException(String message, String formatName, int lineNumber, int columnNumber,
            Throwable cause) {
        super(buildMessage(message, formatName, lineNumber, columnNumber), cause);
        this.formatName = formatName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     * Builds the complete exception message by incorporating the base message,
     * format name, and line/column numbers if available.
     *
     * @param base   the base detail message.
     * @param format the name of the RDF format.
     * @param line   the line number.
     * @param col    the column number.
     * @return the formatted exception message string.
     */
    private static String buildMessage(String base, String format, int line, int col) {
        StringBuilder sb = new StringBuilder(base);
        if (!"unknown".equals(format)) {
            sb.append(" [Format: ").append(format).append("]");
        }
        if (line > 0) {
            sb.append(" at line ").append(line);
            if (col > 0) {
                sb.append(":").append(col);
            }
        }
        return sb.toString();
    }

    /**
     * Returns the name of the RDF format that was being processed when the error
     * occurred.
     *
     * @return the format name, or "unknown" if not specified.
     */
    public String getFormatName() {
        return formatName;
    }

    /**
     * Returns the line number where the error occurred.
     *
     * @return the line number, or -1 if unknown.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the column number where the error occurred.
     *
     * @return the column number, or -1 if unknown.
     */
    public int getColumnNumber() {
        return columnNumber;
    }
}
