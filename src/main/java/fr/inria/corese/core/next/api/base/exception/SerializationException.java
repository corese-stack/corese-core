package fr.inria.corese.core.next.api.base.exception;

/**
 * Exception levée lors d'échecs de sérialisation/désérialisation RDF.
 * Peut contenir des détails spécifiques au format (NTriples, JSON-LD, etc.).
 */
public class SerializationException extends Exception {
    private final String formatName;
    private final int lineNumber;
    private final int columnNumber;

    public SerializationException(String message, String formatName, Throwable cause) {
        this(message, formatName, -1, -1, cause);
    }


    public SerializationException(String message, String formatName, int lineNumber, int columnNumber, Throwable cause) {
        super(buildMessage(message, formatName, lineNumber, columnNumber), cause);
        this.formatName = formatName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

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


    public String getFormatName() {
        return formatName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }
}
