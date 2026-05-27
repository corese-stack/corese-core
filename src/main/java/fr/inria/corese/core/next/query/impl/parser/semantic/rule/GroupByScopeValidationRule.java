package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates that variables referenced from GROUP BY are visible from the query scope.
 */
public final class GroupByScopeValidationRule extends AbstractSemanticValidationRule {

    private static final String DIAGNOSTIC_SOURCE = "GroupByScopeValidationRule";

    @Override
    protected String getDiagnosticSource() {
        return DIAGNOSTIC_SOURCE;
    }

    @Override
    public List<QueryDiagnostic> validate(QueryAst queryAst) {
        Set<String> visibleVariables = collectVisibleVariables(queryAst);
        List<QueryDiagnostic> diagnostics = new ArrayList<>();
        for (TermAst expression : getSolutionModifier(queryAst).groupBy().expressions()) {
            addOutOfScopeDiagnostics(
                    collectReferencedVariables(expression),
                    visibleVariables,
                    ScopeClause.GROUP_BY,
                    diagnostics);
        }
        return List.copyOf(diagnostics);
    }
}
