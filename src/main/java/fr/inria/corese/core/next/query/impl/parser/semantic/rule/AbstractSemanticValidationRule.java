package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;

public abstract class AbstractSemanticValidationRule implements SemanticValidationRule {

    protected abstract String getDiagnosticSource();

    protected QueryDiagnostic buildOutOfScopeDiagnostic(String variableName, String clause) {
        return new QueryDiagnostic(
                QueryDiagnostic.Kind.SEMANTIC_ERROR,
                QueryDiagnostic.Severity.ERROR,
                "Variable ?" + variableName + " used in " + clause + " is not visible in WHERE clause",
                -1,
                -1,
                "?" + variableName,
                getDiagnosticSource());
    }
}
