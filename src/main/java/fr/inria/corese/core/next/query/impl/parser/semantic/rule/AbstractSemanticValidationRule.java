package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;

public abstract class AbstractSemanticValidationRule implements SemanticValidationRule {

    protected abstract String getDiagnosticSource();

    protected QueryDiagnostic buildOutOfScopeDiagnostic(String variableName, String clause) {
        return buildOutOfScopeDiagnostic(getDiagnosticSource(), variableName, clause);
    }

    public static QueryDiagnostic buildOutOfScopeDiagnostic(String source, String variableName, String clause) {
        return new QueryDiagnostic(
                QueryDiagnostic.Kind.SEMANTIC_ERROR,
                QueryDiagnostic.Severity.ERROR,
                "Variable ?" + variableName + " used in " + clause + " is not visible in WHERE clause",
                -1,
                -1,
                "?" + variableName,
                source);
    }

    protected QueryDiagnostic buildIncorrectTypeDiagnostic(String variableName, String clause, String expectedType) {
        return buildIncorrectTypeDiagnostic(getDiagnosticSource(), variableName, clause, expectedType);
    }

    public static QueryDiagnostic buildIncorrectTypeDiagnostic(String source, String variableName, String clause, String expectedType) {
        return new QueryDiagnostic(
                QueryDiagnostic.Kind.SEMANTIC_ERROR,
                QueryDiagnostic.Severity.ERROR,
                variableName + " used in " + clause + " should be resolvable to a " + expectedType,
                -1,
                -1,
                "?" + variableName,
                source);
    }
}
