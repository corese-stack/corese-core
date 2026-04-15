package fr.inria.corese.core.next.query.api.validation;

import java.util.Objects;

/**
 * Public diagnostic model for query validation.
 *
 * <p>This type is intended to unify syntax and semantic diagnostics behind a
 * stable contract consumable by external tools.</p>
 *
 * <p>Line numbers follow the parser conventions already used in the query
 * package: line numbers are 1-based when known, columns are 0-based when
 * known, and both are set to {@code -1} when unavailable.</p>
 */
public record QueryDiagnostic(
        Kind kind,
        Severity severity,
        String message,
        int line,
        int column,
        String offendingText,
        String source
) {

    /** High-level diagnostic category. */
    public enum Kind {
        LEXER_ERROR,
        SYNTAX_ERROR,
        SEMANTIC_ERROR
    }

    /** Diagnostic severity level. */
    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }

    public QueryDiagnostic {
        Objects.requireNonNull(kind, "QueryDiagnostic.kind must not be null");
        Objects.requireNonNull(severity, "QueryDiagnostic.severity must not be null");
        Objects.requireNonNull(message, "QueryDiagnostic.message must not be null");
        if (line < -1) {
            throw new IllegalArgumentException("line must be >= -1");
        }
        if (column < -1) {
            throw new IllegalArgumentException("column must be >= -1");
        }
    }

    /**
     * Formats this diagnostic as a compact single-line message.
     */
    public String format() {
        String where = line >= 1 ? "line " + line + ":" + column + " " : "";
        String off = offendingText != null && !offendingText.isBlank()
                ? " (offending: " + offendingText + ")"
                : "";
        String src = source != null && !source.isBlank() ? " [" + source + "]" : "";
        return where + message + off + src;
    }
}
