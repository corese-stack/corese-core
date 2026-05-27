package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.VariableScopeAnalyzer;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupByAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ProjectionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates SELECT projections in aggregate query levels.
 *
 * <p>For query levels using explicit or implicit grouping, only aggregates and constants
 * may appear in projected expressions. The only exception is that variables projected
 * directly from simple {@code GROUP BY ?var} terms remain legal. In addition,
 * {@code SELECT *} is rejected when an explicit {@code GROUP BY} clause is present.</p>
 */
public final class GroupedSelectProjectionValidationRule extends AbstractSemanticValidationRule {

    private static final String DIAGNOSTIC_SOURCE = "GroupedSelectProjectionValidationRule";

    private final VariableScopeAnalyzer variableScopeAnalyzer = new VariableScopeAnalyzer();

    @Override
    protected String getDiagnosticSource() {
        return DIAGNOSTIC_SOURCE;
    }

    @Override
    public List<QueryDiagnostic> validate(QueryAst queryAst) {
        if (!(queryAst instanceof SelectQueryAst selectQueryAst)) {
            return List.of();
        }
        if (selectQueryAst.projection().selectAll()) {
            return validateSelectAllProjection(selectQueryAst);
        }
        if (!isAggregateQueryLevel(selectQueryAst)) {
            return List.of();
        }
        if (selectQueryAst.solutionModifier().hasGroupBy() && !hasSemanticallyVisibleGroupBy(selectQueryAst)) {
            return List.of();
        }

        ProjectionAst projection = selectQueryAst.projection();
        GroupByAst groupBy = selectQueryAst.solutionModifier().groupBy();
        Set<String> groupedVariables = collectDirectGroupedVariables(groupBy);
        List<QueryDiagnostic> diagnostics = new ArrayList<>();

        for (VarAst projectedVar : projection.variables()) {
            if (projection.expressionBoundVariables().contains(projectedVar.name())) {
                validateProjectionExpression(
                        projectedVar.name(),
                        projection,
                        diagnostics);
                continue;
            }

            if (!groupedVariables.contains(projectedVar.name())) {
                diagnostics.add(buildGroupedProjectionDiagnostic(projectedVar.name()));
            }
        }

        return List.copyOf(diagnostics);
    }

    private List<QueryDiagnostic> validateSelectAllProjection(SelectQueryAst selectQueryAst) {
        if (!selectQueryAst.solutionModifier().hasGroupBy()) {
            return List.of();
        }
        if (!hasSemanticallyVisibleGroupBy(selectQueryAst)) {
            return List.of();
        }
        return List.of(buildSelectAllGroupedDiagnostic());
    }

    private boolean isAggregateQueryLevel(SelectQueryAst selectQueryAst) {
        if (selectQueryAst.solutionModifier().hasGroupBy()) {
            return true;
        }

        ProjectionAst projection = selectQueryAst.projection();
        for (String variableName : projection.expressionBoundVariables()) {
            if (variableScopeAnalyzer.containsAggregate(projection.expressionTerms().get(variableName))) {
                return true;
            }
        }

        return selectQueryAst.solutionModifier().having().conditions().stream()
                .anyMatch(variableScopeAnalyzer::containsAggregate)
                || selectQueryAst.solutionModifier().orderBy().stream()
                .anyMatch(orderCondition -> variableScopeAnalyzer.containsAggregate(orderCondition.expression()));
    }

    private boolean hasSemanticallyVisibleGroupBy(SelectQueryAst selectQueryAst) {
        Set<String> visibleVariables = collectVisibleVariables(selectQueryAst);
        for (TermAst expression : selectQueryAst.solutionModifier().groupBy().expressions()) {
            for (String referencedVariable : collectReferencedVariables(expression)) {
                if (!visibleVariables.contains(referencedVariable)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void validateProjectionExpression(
            String projectionVariableName,
            ProjectionAst projection,
            List<QueryDiagnostic> diagnostics
    ) {
        TermAst expression = projection.expressionTerms().get(projectionVariableName);
        if (expression == null) {
            return;
        }
        for (String referencedVariable : variableScopeAnalyzer.collectReferencedVariablesOutsideAggregates(expression)) {
            diagnostics.add(buildGroupedProjectionDiagnostic(referencedVariable));
        }
    }

    private Set<String> collectDirectGroupedVariables(GroupByAst groupBy) {
        Set<String> groupedVariables = new LinkedHashSet<>();
        for (TermAst expression : groupBy.expressions()) {
            if (expression instanceof VarAst(String name)) {
                groupedVariables.add(name);
            }
        }
        return groupedVariables;
    }

    private QueryDiagnostic buildGroupedProjectionDiagnostic(String variableName) {
        return new QueryDiagnostic(
                QueryDiagnostic.Kind.SEMANTIC_ERROR,
                QueryDiagnostic.Severity.ERROR,
                "Variable ?" + variableName + " used in SELECT projection must be grouped or aggregated",
                -1,
                -1,
                "?" + variableName,
                getDiagnosticSource());
    }

    private QueryDiagnostic buildSelectAllGroupedDiagnostic() {
        return new QueryDiagnostic(
                QueryDiagnostic.Kind.SEMANTIC_ERROR,
                QueryDiagnostic.Severity.ERROR,
                "SELECT * is not permitted with GROUP BY",
                -1,
                -1,
                "*",
                getDiagnosticSource());
    }
}
