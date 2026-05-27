package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.sparql.ast.AskQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstructQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DescribeQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.OrderConditionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ProjectionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SolutionModifierAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;

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

    @Override
    protected String getDiagnosticSource() {
        return DIAGNOSTIC_SOURCE;
    }

    @Override
    public List<QueryDiagnostic> validate(QueryAst queryAst) {
        return switch (queryAst) {
            case SelectQueryAst selectQueryAst -> validateSelectQuery(selectQueryAst);
            case ConstructQueryAst constructQueryAst -> validateOrderByOnly(constructQueryAst);
            case DescribeQueryAst describeQueryAst -> validateOrderByOnly(describeQueryAst);
            case AskQueryAst askQueryAst -> validateOrderByOnly(askQueryAst);
        };
    }

    private List<QueryDiagnostic> validateSelectQuery(SelectQueryAst queryAst) {
        Set<String> visibleVariables = collectVisibleVariables(queryAst);
        List<QueryDiagnostic> diagnostics = new ArrayList<>();
        validateOrderVariables(
                collectOrderByAvailableVariables(queryAst, visibleVariables),
                queryAst.solutionModifier(),
                diagnostics);
        return List.copyOf(diagnostics);
    }

    private List<QueryDiagnostic> validateOrderByOnly(QueryAst queryAst) {
        List<QueryDiagnostic> diagnostics = new ArrayList<>();
        Set<String> visibleVariables = collectVisibleVariables(queryAst);
        validateOrderVariables(visibleVariables, getSolutionModifier(queryAst), diagnostics);
        return List.copyOf(diagnostics);
    }

    /**
     * ORDER BY is applied before the final projection step. For the current
     * feature set, the variables visible from the WHERE clause remain the source
     * of truth. Explicit projection variables and GROUP BY aliases are added
     * because they can legally be reused by ORDER BY. Stricter aggregate-query
     * semantics are enforced separately by {@link GroupedOrderByValidationRule}.
     */
    private Set<String> collectOrderByAvailableVariables(
            SelectQueryAst queryAst,
            Set<String> visibleVariables
    ) {
        Set<String> availableVariables = new LinkedHashSet<>(visibleVariables);
        availableVariables.addAll(queryAst.solutionModifier().groupBy().expressionBoundVariables());
        ProjectionAst projection = queryAst.projection();
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
            addOutOfScopeDiagnostics(
                    collectReferencedVariables(orderCondition.expression()),
                    availableOrderVariables,
                    ScopeClause.ORDER_BY,
                    diagnostics);
        }
    }
}
