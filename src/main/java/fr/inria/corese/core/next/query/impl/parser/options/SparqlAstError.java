package fr.inria.corese.core.next.query.impl.parser.options;

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
     * @param kind the high-level category of the error
     *             (e.g. SYNTAX, SEMANTIC, LEXICAL)
     * @param severity the severity level of the error
     *                 (e.g. ERROR, WARNING)
     * @param message a human-readable description of the problem;
     *                must not be {@code null}
     * @param line the line number where the error occurred (1-based, must be ≥ 0)
     * @param column the column position within the line (0-based, must be ≥ 0)
     * @param offendingText the fragment of input that caused the error,
     *                      may be {@code null} if unavailable
     * @param source an optional source identifier (e.g. file name or query identifier),
     *               may be {@code null}
     *
     * @throws NullPointerException if {@code kind}, {@code severity}, or {@code message} is {@code null}
     * @throws IllegalArgumentException if {@code line} or {@code column} is negative
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
