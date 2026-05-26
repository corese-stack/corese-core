package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.VariableScopeAnalyzer;
import fr.inria.corese.core.next.query.impl.sparql.ast.ProjectionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates that explicitly projected SELECT variables are visible from the
 * WHERE clause scope.
 */
public final class SelectProjectionScopeValidationRule extends AbstractSemanticValidationRule {

    private static final String DIAGNOSTIC_SOURCE = "SelectProjectionScopeValidationRule";

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
            return List.of();
        }

        Set<String> visibleVariables = variableScopeAnalyzer.collectVisibleVariables(selectQueryAst);
        List<QueryDiagnostic> diagnostics = new ArrayList<>();
        validateProjectionVariables(selectQueryAst.projection(), visibleVariables, diagnostics);
        return List.copyOf(diagnostics);
    }

    private void validateProjectionVariables(
            ProjectionAst projection,
            Set<String> visibleVariables,
            List<QueryDiagnostic> diagnostics
    ) {
        for (VarAst projectedVar : projection.variables()) {
            if (projection.expressionBoundVariables().contains(projectedVar.name())) {
                validateProjectionExpression(projectedVar.name(), projection, visibleVariables, diagnostics);
                continue;
            }
            if (!visibleVariables.contains(projectedVar.name())) {
                diagnostics.add(buildOutOfScopeDiagnostic(projectedVar.name(), "SELECT projection"));
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
        for (String referencedVariable : projection.expressionReferencedVariables()
                .getOrDefault(projectionVariableName, Set.of())) {
            if (!visibleVariables.contains(referencedVariable)) {
                diagnostics.add(buildOutOfScopeDiagnostic(referencedVariable, "SELECT projection"));
            }
        }
    }
}
