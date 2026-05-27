package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates HAVING expressions in aggregate query levels.
 *
 * <p>Variables referenced outside aggregate calls must belong to the grouping key,
 * either as simple {@code GROUP BY ?var} variables or as aliases introduced by
 * {@code GROUP BY (expr AS ?var)}.</p>
 */
public final class GroupedHavingValidationRule extends AbstractSemanticValidationRule {

    private static final String DIAGNOSTIC_SOURCE = "GroupedHavingValidationRule";

    @Override
    protected String getDiagnosticSource() {
        return DIAGNOSTIC_SOURCE;
    }

    @Override
    public List<QueryDiagnostic> validate(QueryAst queryAst) {
        if (!getSolutionModifier(queryAst).hasHaving()) {
            return List.of();
        }
        if (!isAggregateQueryLevel(queryAst)) {
            return List.of();
        }
        if (getSolutionModifier(queryAst).hasGroupBy() && !hasSemanticallyVisibleGroupBy(queryAst)) {
            return List.of();
        }

        Set<String> availableVariables = collectVisibleVariables(queryAst);
        availableVariables.addAll(getSolutionModifier(queryAst).groupBy().expressionBoundVariables());
        Set<String> groupedVariables = collectGroupedVariableNames(getSolutionModifier(queryAst).groupBy());

        List<QueryDiagnostic> diagnostics = new ArrayList<>();
        for (TermAst condition : getSolutionModifier(queryAst).having().conditions()) {
            for (String referencedVariable : collectReferencedVariablesOutsideAggregates(condition)) {
                if (!availableVariables.contains(referencedVariable)) {
                    continue;
                }
                if (!groupedVariables.contains(referencedVariable)) {
                    diagnostics.add(buildGroupedHavingDiagnostic(referencedVariable));
                }
            }
        }
        return List.copyOf(diagnostics);
    }

    private QueryDiagnostic buildGroupedHavingDiagnostic(String variableName) {
        return new QueryDiagnostic(
                QueryDiagnostic.Kind.SEMANTIC_ERROR,
                QueryDiagnostic.Severity.ERROR,
                "Variable ?" + variableName + " used in HAVING must be grouped or aggregated",
                -1,
                -1,
                "?" + variableName,
                getDiagnosticSource());
    }
}
