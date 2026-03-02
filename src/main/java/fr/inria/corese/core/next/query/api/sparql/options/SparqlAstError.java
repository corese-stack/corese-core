package fr.inria.corese.core.next.query.api.sparql.options;

import java.util.Objects;

public record SparqlAstError(
        Kind kind,
        Severity severity,
        String message,
        int line,
        int column,
        String offendingText,
        String source
) {
    /**
     * High-level error category.
     */
    public enum Kind {
        /** Lexer/tokenization error. */
        LEXER_ERROR,
        /** Parser syntax error. */
        SYNTAX_ERROR,
        /** Strict-mode / extra validation error. */
        STRICT_MODE_ERROR
    }

    /**
     * Diagnostic severity.
     */
    public enum Severity {
        INFO, WARNING, ERROR
    }

    /**
     * Constructor
     * @param kind
     * @param severity
     * @param message
     * @param line
     * @param column
     * @param offendingText
     * @param source
     */
    public SparqlAstError {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
        // line/column: ANTLR gives 1-based line, 0-based column typically; keep as-is.
        if (line < 0) throw new IllegalArgumentException("line must be >= 0");
        if (column < 0) throw new IllegalArgumentException("column must be >= 0");
    }

    /**
     * Formats this diagnostic as a human-friendly single-line string.
     */
    public String format() {
        String where = (line > 0) ? ("line " + line + ":" + column + " ") : "";
        String off = (offendingText != null && !offendingText.isBlank())
                ? (" (offending: " + offendingText + ")")
                : "";
        String src = (source != null && !source.isBlank()) ? (" [" + source + "]") : "";
        return where + message + off + src;
    }
}
