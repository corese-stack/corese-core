package fr.inria.corese.core.next.query.impl.sparql.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates aliases introduced by explicit SELECT expressions.
 *
 * <p>A projected variable, or a variable referenced by a SELECT expression, may be
 * unbound. The expression then evaluates to an error for that solution. In contrast,
 * the target variable of {@code (expr AS ?var)} must be new at the point where it is
 * introduced.</p>
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

        Set<String> inScopeVariables = collectSelectAvailableVariables(selectQueryAst);
        return selectQueryAst.projection().expressionBoundVariables().stream()
                .filter(inScopeVariables::contains)
                .map(this::buildAlreadyInScopeDiagnostic)
                .toList();
    }

    /**
     * GROUP BY aliases are already in scope and cannot be reused as SELECT expression targets.
     */
    private Set<String> collectSelectAvailableVariables(SelectQueryAst selectQueryAst) {
        Set<String> visibleVariables = new LinkedHashSet<>(collectVisibleVariables(selectQueryAst));
        visibleVariables.addAll(selectQueryAst.solutionModifier().groupBy().expressionBoundVariables());
        return visibleVariables;
    }

    private QueryDiagnostic buildAlreadyInScopeDiagnostic(String variableName) {
        return new QueryDiagnostic(
                QueryDiagnostic.Kind.SEMANTIC_ERROR,
                QueryDiagnostic.Severity.ERROR,
                "Variable ?" + variableName + " introduced by SELECT expression is already in scope",
                -1,
                -1,
                "?" + variableName,
                getDiagnosticSource());
    }

}
