package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.VariableScopeAnalyzer;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates that variables referenced from ORDER BY are visible from the
 * WHERE clause scope.
 */
public final class OrderByScopeValidationRule extends AbstractSemanticValidationRule {

    private static final String DIAGNOSTIC_SOURCE = "OrderByScopeValidationRule";

    private final VariableScopeAnalyzer variableScopeAnalyzer = new VariableScopeAnalyzer();

    @Override
    protected String getDiagnosticSource() {
        return DIAGNOSTIC_SOURCE;
    }

    @Override
    public List<QueryDiagnostic> validate(QueryAst queryAst) {
        return switch (queryAst) {
            case SelectQueryAst selectQueryAst -> validateSelectQuery(selectQueryAst);
            case ConstructQueryAst constructQueryAst -> validateOrderByOnly(
                    constructQueryAst.whereClause(),
                    constructQueryAst.solutionModifier());
            case DescribeQueryAst describeQueryAst -> validateOrderByOnly(
                    describeQueryAst.whereClause(),
                    describeQueryAst.solutionModifier());
            // TODO: handle ASK solution modifiers with SPARQL 1.1 support.
            case AskQueryAst ignored -> List.of();
            case LoadQueryAst ignored -> List.of();
            case ClearQueryAst ignored -> List.of();
        };
    }

    private List<QueryDiagnostic> validateSelectQuery(SelectQueryAst queryAst) {
        Set<String> visibleVariables = variableScopeAnalyzer.collectVisibleVariables(queryAst);
        List<QueryDiagnostic> diagnostics = new ArrayList<>();
        validateOrderVariables(
                collectOrderByAvailableVariables(queryAst.projection(), visibleVariables),
                queryAst.solutionModifier(),
                diagnostics);
        return List.copyOf(diagnostics);
    }

    private List<QueryDiagnostic> validateOrderByOnly(
            GroupGraphPatternAst whereClause,
            SolutionModifierAst solutionModifier
    ) {
        List<QueryDiagnostic> diagnostics = new ArrayList<>();
        Set<String> visibleVariables = variableScopeAnalyzer.collectVisibleVariables(whereClause);
        validateOrderVariables(visibleVariables, solutionModifier, diagnostics);
        return List.copyOf(diagnostics);
    }

    /**
     * ORDER BY is applied before the final projection step. For the current
     * feature set, the variables visible from the WHERE clause remain the source
     * of truth, and explicit projection variables are added to keep the rule
     * ready for SELECT-specific extensions.
     */
    private Set<String> collectOrderByAvailableVariables(
            ProjectionAst projection,
            Set<String> visibleVariables
    ) {
        Set<String> availableVariables = new LinkedHashSet<>(visibleVariables);
        // TODO: handle SELECT aliases, GROUP BY, and HAVING with SPARQL 1.1 support.
        if (!projection.selectAll()) {
            for (VarAst projectedVar : projection.variables()) {
                availableVariables.add(projectedVar.name());
            }
        }
        return availableVariables;
    }

    private void validateOrderVariables(
            Set<String> availableOrderVariables,
            SolutionModifierAst solutionModifier,
            List<QueryDiagnostic> diagnostics
    ) {
        for (OrderConditionAst orderCondition : solutionModifier.orderBy()) {
            Set<String> referencedVariables = variableScopeAnalyzer
                    .collectReferencedVariables(orderCondition.expression());

            for (String variableName : referencedVariables) {
                if (!availableOrderVariables.contains(variableName)) {
                    diagnostics.add(buildOutOfScopeDiagnostic(variableName, "ORDER BY"));
                }
            }
        }
    }
}
