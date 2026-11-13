package fr.inria.corese.core.next.query.impl.sparql.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.sparql.ast.visitor.AbstractAstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.SemanticValidationUtils.hasIncompatibleComparison;
import static fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.SemanticValidationUtils.isConstantBooleanCondition;
import static fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.SemanticValidationUtils.isPotentialBooleanCompatible;
import static fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.SemanticValidationUtils.isSuspiciousEBVCoercion;

/**
 * Validates that FILTER and HAVING expressions are compatible with SPARQL
 * effective boolean value evaluation.
 */
public final class FilterArgumentsValidationRule extends AbstractSemanticValidationRule {

    @Override
    protected String getDiagnosticSource() {
        return FilterArgumentsValidationRule.class.getSimpleName();
    }

    @Override
    public List<QueryDiagnostic> validate(QueryAst queryAst) {
        FilterArgumentsValidationVisitor visitor = new FilterArgumentsValidationVisitor();
        queryAst.accept(visitor);
        return visitor.getResult();
    }

    private class FilterArgumentsValidationVisitor extends AbstractAstVisitor {
        private final List<QueryDiagnostic> result = new ArrayList<>();

        public List<QueryDiagnostic> getResult() {
            return result;
        }

        @Override
        public void visit(PatternAst patternAst) {
            if (patternAst instanceof FilterAst(TermAst operator, int line, int column)) {
                addConditionDiagnostics(operator, "FILTER", line, column);
            }
        }

        @Override
        public void visit(HavingAst havingAst) {
            for (int i = 0; i < havingAst.conditions().size(); i++) {
                TermAst condition = havingAst.conditions().get(i);
                HavingAst.SourceLocation location = i < havingAst.conditionLocations().size()
                        ? havingAst.conditionLocations().get(i)
                        : new HavingAst.SourceLocation(havingAst.line(), havingAst.column());
                addConditionDiagnostics(condition, "HAVING", location.line(), location.column());
            }
        }

        private void addConditionDiagnostics(TermAst condition, String clause, int line, int column) {
            if (hasIncompatibleComparison(condition)) {
                result.add(buildAdvisoryDiagnostic(
                        "Incompatible operand types in comparison used in " + clause,
                        condition.getName(),
                        line,
                        column));
            } else if (isConstantBooleanCondition(condition)) {
                result.add(buildAdvisoryDiagnostic(
                        "Constant condition in " + clause + "; verify that this is intentional",
                        condition.getName(),
                        line,
                        column));
            } else if (!isPotentialBooleanCompatible(condition) || isSuspiciousEBVCoercion(condition)) {
                result.add(buildSuspiciousEBVDiagnostic(condition.getName(), clause, line, column));
            }
        }
    }
}
