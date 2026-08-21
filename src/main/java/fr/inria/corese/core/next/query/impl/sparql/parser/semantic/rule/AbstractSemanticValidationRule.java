package fr.inria.corese.core.next.query.impl.sparql.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.VariableScopeAnalyzer;
import fr.inria.corese.core.next.query.impl.sparql.ast.AskQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstructQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DescribeQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupByAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SolutionModifierAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;

import java.util.LinkedHashSet;
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
            case UpdateRequestAst ignored -> SolutionModifierAst.empty();
        };
    }

    protected final Set<String> collectVisibleVariables(QueryAst queryAst) {
        return variableScopeAnalyzer.collectVisibleVariables(queryAst);
    }

    protected final Set<String> collectReferencedVariables(TermAst term) {
        return variableScopeAnalyzer.collectReferencedVariables(term);
    }

    protected final Set<String> collectReferencedVariablesOutsideAggregates(TermAst term) {
        return variableScopeAnalyzer.collectReferencedVariablesOutsideAggregates(term);
    }

    protected final boolean containsAggregate(TermAst term) {
        return variableScopeAnalyzer.containsAggregate(term);
    }

    protected final boolean isAggregateQueryLevel(QueryAst queryAst) {
        if (getSolutionModifier(queryAst).hasGroupBy()) {
            return true;
        }
        if (queryAst instanceof SelectQueryAst selectQueryAst) {
            for (String variableName : selectQueryAst.projection().expressionBoundVariables()) {
                if (containsAggregate(selectQueryAst.projection().expressionTerms().get(variableName))) {
                    return true;
                }
            }
        }
        return getSolutionModifier(queryAst).having().conditions().stream().anyMatch(this::containsAggregate)
                || getSolutionModifier(queryAst).orderBy().stream()
                .anyMatch(orderCondition -> containsAggregate(orderCondition.expression()));
    }

    protected final boolean hasSemanticallyVisibleGroupBy(QueryAst queryAst) {
        Set<String> visibleVariables = collectVisibleVariables(queryAst);
        for (TermAst expression : getSolutionModifier(queryAst).groupBy().expressions()) {
            for (String referencedVariable : collectReferencedVariables(expression)) {
                if (!visibleVariables.contains(referencedVariable)) {
                    return false;
                }
            }
        }
        return true;
    }

    protected final Set<String> collectGroupedVariableNames(GroupByAst groupByAst) {
        Set<String> groupedVariables = new LinkedHashSet<>(groupByAst.expressionBoundVariables());
        for (TermAst expression : groupByAst.expressions()) {
            if (expression instanceof VarAst(String name)) {
                groupedVariables.add(name);
            }
        }
        return groupedVariables;
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
        return buildOutOfScopeDiagnostic(getDiagnosticSource(), variableName, clause.label());
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
                variableName,
                source);
    }

}
