package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.sparql.ast.OrderConditionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates ORDER BY expressions in aggregate query levels.
 *
 * <p>Variables referenced outside aggregate calls must belong to the grouping key,
 * unless they designate a projected variable already made available to ORDER BY
 * at the query level.</p>
 */
public final class GroupedOrderByValidationRule extends AbstractSemanticValidationRule {

    private static final String DIAGNOSTIC_SOURCE = "GroupedOrderByValidationRule";

    @Override
    protected String getDiagnosticSource() {
        return DIAGNOSTIC_SOURCE;
    }

    @Override
    public List<QueryDiagnostic> validate(QueryAst queryAst) {
        if (!getSolutionModifier(queryAst).hasOrderBy()) {
            return List.of();
        }
        if (!isAggregateQueryLevel(queryAst)) {
            return List.of();
        }
        if (getSolutionModifier(queryAst).hasGroupBy() && !hasSemanticallyVisibleGroupBy(queryAst)) {
            return List.of();
        }

        Set<String> availableVariables = collectOrderByAvailableVariables(queryAst);
        Set<String> groupedVariables = collectGroupedVariableNames(getSolutionModifier(queryAst).groupBy());
        Set<String> projectedVariables = collectProjectedVariables(queryAst);

        List<QueryDiagnostic> diagnostics = new ArrayList<>();
        for (OrderConditionAst orderCondition : getSolutionModifier(queryAst).orderBy()) {
            for (String referencedVariable : collectReferencedVariablesOutsideAggregates(orderCondition.expression())) {
                boolean isKnownVariable = availableVariables.contains(referencedVariable);
                boolean isAllowedAggregateOrderVariable =
                        groupedVariables.contains(referencedVariable) || projectedVariables.contains(referencedVariable);
                if (isKnownVariable && !isAllowedAggregateOrderVariable) {
                    diagnostics.add(buildGroupedOrderByDiagnostic(referencedVariable));
                }
            }
        }
        return List.copyOf(diagnostics);
    }

    private Set<String> collectOrderByAvailableVariables(QueryAst queryAst) {
        Set<String> availableVariables = new LinkedHashSet<>(collectVisibleVariables(queryAst));
        availableVariables.addAll(getSolutionModifier(queryAst).groupBy().expressionBoundVariables());
        availableVariables.addAll(collectProjectedVariables(queryAst));
        return availableVariables;
    }

    private Set<String> collectProjectedVariables(QueryAst queryAst) {
        Set<String> projectedVariables = new LinkedHashSet<>();
        if (queryAst instanceof SelectQueryAst selectQueryAst && !selectQueryAst.projection().selectAll()) {
            for (VarAst variable : selectQueryAst.projection().variables()) {
                projectedVariables.add(variable.name());
            }
        }
        return projectedVariables;
    }

    private QueryDiagnostic buildGroupedOrderByDiagnostic(String variableName) {
        return new QueryDiagnostic(
                QueryDiagnostic.Kind.SEMANTIC_ERROR,
                QueryDiagnostic.Severity.ERROR,
                "Variable ?" + variableName + " used in ORDER BY must be grouped or aggregated",
                -1,
                -1,
                "?" + variableName,
                getDiagnosticSource());
    }
}
