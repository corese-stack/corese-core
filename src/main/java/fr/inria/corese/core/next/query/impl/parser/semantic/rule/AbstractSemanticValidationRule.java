package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.VariableScopeAnalyzer;
import fr.inria.corese.core.next.query.impl.sparql.ast.AskQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstructQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DescribeQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SolutionModifierAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;
import java.util.Set;

/**
 * Shared helpers for semantic validation rules that enforce SPARQL variable scope.
 */
public abstract class AbstractSemanticValidationRule implements SemanticValidationRule {

    private final VariableScopeAnalyzer variableScopeAnalyzer = new VariableScopeAnalyzer();

    protected abstract String getDiagnosticSource();

    /**
     * Returns the solution modifiers carried by the given query form.
     */
    protected final SolutionModifierAst getSolutionModifier(QueryAst queryAst) {
        return switch (queryAst) {
            case SelectQueryAst selectQueryAst -> selectQueryAst.solutionModifier();
            case ConstructQueryAst constructQueryAst -> constructQueryAst.solutionModifier();
            case DescribeQueryAst describeQueryAst -> describeQueryAst.solutionModifier();
            case AskQueryAst askQueryAst -> askQueryAst.solutionModifier();
        };
    }

    protected final Set<String> collectVisibleVariables(QueryAst queryAst) {
        return variableScopeAnalyzer.collectVisibleVariables(queryAst);
    }

    protected final Set<String> collectVisibleVariables(GroupGraphPatternAst whereClause) {
        return variableScopeAnalyzer.collectVisibleVariables(whereClause);
    }

    protected final Set<String> collectReferencedVariables(TermAst term) {
        return variableScopeAnalyzer.collectReferencedVariables(term);
    }

    protected final void addOutOfScopeDiagnostics(
            Iterable<String> referencedVariables,
            Set<String> visibleVariables,
            ScopeClause clause,
            List<QueryDiagnostic> diagnostics
    ) {
        for (String variableName : referencedVariables) {
            if (!visibleVariables.contains(variableName)) {
                diagnostics.add(buildOutOfScopeDiagnostic(variableName, clause));
            }
        }
    }

    protected QueryDiagnostic buildOutOfScopeDiagnostic(String variableName, ScopeClause clause) {
        return new QueryDiagnostic(
                QueryDiagnostic.Kind.SEMANTIC_ERROR,
                QueryDiagnostic.Severity.ERROR,
                "Variable ?" + variableName + " used in " + clause.label() + " is not visible in WHERE clause",
                -1,
                -1,
                "?" + variableName,
                getDiagnosticSource());
    }
}
