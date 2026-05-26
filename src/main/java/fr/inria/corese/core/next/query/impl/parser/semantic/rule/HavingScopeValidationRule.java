package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.VariableScopeAnalyzer;
import fr.inria.corese.core.next.query.impl.sparql.ast.AskQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstructQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DescribeQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SolutionModifierAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates that variables referenced from HAVING are visible from the query scope.
 */
public final class HavingScopeValidationRule extends AbstractSemanticValidationRule {

    private static final String DIAGNOSTIC_SOURCE = "HavingScopeValidationRule";

    private final VariableScopeAnalyzer variableScopeAnalyzer = new VariableScopeAnalyzer();

    @Override
    protected String getDiagnosticSource() {
        return DIAGNOSTIC_SOURCE;
    }

    @Override
    public List<QueryDiagnostic> validate(QueryAst queryAst) {
        SolutionModifierAst solutionModifier = switch (queryAst) {
            case SelectQueryAst selectQueryAst -> selectQueryAst.solutionModifier();
            case ConstructQueryAst constructQueryAst -> constructQueryAst.solutionModifier();
            case DescribeQueryAst describeQueryAst -> describeQueryAst.solutionModifier();
            case AskQueryAst askQueryAst -> askQueryAst.solutionModifier();
        };

        Set<String> visibleVariables = variableScopeAnalyzer.collectVisibleVariables(queryAst);
        List<QueryDiagnostic> diagnostics = new ArrayList<>();
        for (TermAst condition : solutionModifier.having().conditions()) {
            for (String variableName : variableScopeAnalyzer.collectReferencedVariables(condition)) {
                if (!visibleVariables.contains(variableName)) {
                    diagnostics.add(buildOutOfScopeDiagnostic(variableName, "HAVING"));
                }
            }
        }
        return List.copyOf(diagnostics);
    }
}
