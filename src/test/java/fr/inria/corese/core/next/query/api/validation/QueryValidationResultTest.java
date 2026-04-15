package fr.inria.corese.core.next.query.api.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryValidationResultTest {

    @Test
    void isValidWhenNoDiagnosticsArePresent() {
        QueryValidationResult result = new QueryValidationResult(List.of());

        assertTrue(result.isValid());
    }

    @Test
    void isValidWhenOnlyInfoDiagnosticsArePresent() {
        QueryValidationResult result = new QueryValidationResult(List.of(
                new QueryDiagnostic(
                        QueryDiagnostic.Kind.SEMANTIC_ERROR,
                        QueryDiagnostic.Severity.INFO,
                        "Informational diagnostic",
                        -1,
                        -1,
                        null,
                        "test")));

        assertTrue(result.isValid());
    }

    @Test
    void isValidWhenOnlyWarningDiagnosticsArePresent() {
        QueryValidationResult result = new QueryValidationResult(List.of(
                new QueryDiagnostic(
                        QueryDiagnostic.Kind.SEMANTIC_ERROR,
                        QueryDiagnostic.Severity.WARNING,
                        "Warning diagnostic",
                        -1,
                        -1,
                        null,
                        "test")));

        assertTrue(result.isValid());
    }

    @Test
    void isInvalidWhenAtLeastOneErrorDiagnosticIsPresent() {
        QueryValidationResult result = new QueryValidationResult(List.of(
                new QueryDiagnostic(
                        QueryDiagnostic.Kind.SEMANTIC_ERROR,
                        QueryDiagnostic.Severity.WARNING,
                        "Warning diagnostic",
                        -1,
                        -1,
                        null,
                        "test"),
                new QueryDiagnostic(
                        QueryDiagnostic.Kind.SEMANTIC_ERROR,
                        QueryDiagnostic.Severity.ERROR,
                        "Error diagnostic",
                        -1,
                        -1,
                        null,
                        "test")));

        assertFalse(result.isValid());
    }
}
