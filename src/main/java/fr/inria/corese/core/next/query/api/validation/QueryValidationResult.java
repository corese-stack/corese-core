package fr.inria.corese.core.next.query.api.validation;

import java.util.List;

/**
 * Aggregated validation result for a query.
 *
 * <p>A result is considered valid when it contains no diagnostic with severity
 * {@link QueryDiagnostic.Severity#ERROR}. Informational or warning diagnostics
 * may still be present on a valid result.</p>
 */
public record QueryValidationResult(List<QueryDiagnostic> diagnostics) {

    public QueryValidationResult {
        diagnostics = diagnostics != null ? List.copyOf(diagnostics) : List.of();
    }

    /** Returns {@code true} when no error diagnostic was reported. */
    public boolean isValid() {
        return diagnostics.stream()
                .noneMatch(diagnostic -> diagnostic.severity() == QueryDiagnostic.Severity.ERROR);
    }
}
