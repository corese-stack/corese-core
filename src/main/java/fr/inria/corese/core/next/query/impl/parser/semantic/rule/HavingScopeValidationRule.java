package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates that variables referenced from HAVING are visible from the query scope.
 */
public final class HavingScopeValidationRule extends AbstractSemanticValidationRule {

    private static final String DIAGNOSTIC_SOURCE = "HavingScopeValidationRule";

    @Override
    protected String getDiagnosticSource() {
        return DIAGNOSTIC_SOURCE;
    }

    @Override
    public List<QueryDiagnostic> validate(QueryAst queryAst) {
        Set<String> visibleVariables = collectVisibleVariables(queryAst);
        List<QueryDiagnostic> diagnostics = new ArrayList<>();
        for (TermAst condition : getSolutionModifier(queryAst).having().conditions()) {
            addOutOfScopeDiagnostics(
                    collectReferencedVariables(condition),
                    visibleVariables,
                    ScopeClause.HAVING,
                    diagnostics);
        }
        return List.copyOf(diagnostics);
    }
}
