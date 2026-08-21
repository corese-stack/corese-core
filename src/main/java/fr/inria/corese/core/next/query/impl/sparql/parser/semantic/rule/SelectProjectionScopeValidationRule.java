package fr.inria.corese.core.next.query.impl.sparql.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.sparql.ast.ProjectionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates that explicitly projected SELECT variables are visible from the
 * WHERE clause scope.
 */
public final class SelectProjectionScopeValidationRule extends AbstractSemanticValidationRule {

    private static final String DIAGNOSTIC_SOURCE = "SelectProjectionScopeValidationRule";

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
            return List.of();
        }

        Set<String> visibleVariables = collectSelectAvailableVariables(selectQueryAst);
        List<QueryDiagnostic> diagnostics = new ArrayList<>();
        validateProjectionVariables(selectQueryAst.projection(), visibleVariables, diagnostics);
        return List.copyOf(diagnostics);
    }

    /**
     * SELECT projections can reuse aliases introduced by {@code GROUP BY (expr AS ?var)}.
     */
    private Set<String> collectSelectAvailableVariables(SelectQueryAst selectQueryAst) {
        Set<String> visibleVariables = new LinkedHashSet<>(collectVisibleVariables(selectQueryAst));
        visibleVariables.addAll(selectQueryAst.solutionModifier().groupBy().expressionBoundVariables());
        return visibleVariables;
    }

    private void validateProjectionVariables(
            ProjectionAst projection,
            Set<String> visibleVariables,
            List<QueryDiagnostic> diagnostics
    ) {
        Set<String> availableVariables = new LinkedHashSet<>(visibleVariables);
        for (VarAst projectedVar : projection.variables()) {
            if (projection.expressionBoundVariables().contains(projectedVar.name())) {
                validateProjectionExpression(projectedVar.name(), projection, availableVariables, diagnostics);
                availableVariables.add(projectedVar.name());
                continue;
            }
            if (!availableVariables.contains(projectedVar.name())) {
                diagnostics.add(buildOutOfScopeDiagnostic(projectedVar.name(), ScopeClause.SELECT_PROJECTION));
            }
        }
    }

    /**
     * SELECT expressions introduce the projected variable themselves, but the variables they reference
     * must still be visible from the query scope.
     */
    private void validateProjectionExpression(
            String projectionVariableName,
            ProjectionAst projection,
            Set<String> visibleVariables,
            List<QueryDiagnostic> diagnostics
    ) {
        addOutOfScopeDiagnostics(
                projection.expressionReferencedVariables().getOrDefault(projectionVariableName, Set.of()),
                visibleVariables,
                ScopeClause.SELECT_PROJECTION,
                diagnostics);
    }
}
