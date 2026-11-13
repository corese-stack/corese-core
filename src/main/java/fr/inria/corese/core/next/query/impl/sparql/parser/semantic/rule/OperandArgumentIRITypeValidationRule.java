package fr.inria.corese.core.next.query.impl.sparql.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.HavingAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.PatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.visitor.AbstractAstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.SemanticValidationUtils.isPotentialIri;
import static fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.SemanticValidationUtils.visitExpressionTerms;

/**
 * Check that the comparison operand do not compare IRIs (except for the different != operator)
 */
public class OperandArgumentIRITypeValidationRule extends AbstractSemanticValidationRule {

    @Override
    protected String getDiagnosticSource() {
        return OperandArgumentIRITypeValidationRule.class.getSimpleName();
    }

    @Override
    public List<QueryDiagnostic> validate(QueryAst queryAst) {
        OperandIRIArgumentTypeVisitor visitor = new OperandIRIArgumentTypeVisitor();
        queryAst.accept(visitor);
        return visitor.getResult();
    }

    private class OperandIRIArgumentTypeVisitor extends AbstractAstVisitor {
        private final List<QueryDiagnostic> result = new ArrayList<>();

        public List<QueryDiagnostic> getResult() {
            return result;
        }

        @Override
        public void visit(PatternAst patternAst) {
            if (patternAst instanceof FilterAst(TermAst operator, int line, int column)) {
                validateExpression(operator, line, column);
            }
        }

        @Override
        public void visit(HavingAst havingAst) {
            for (int i = 0; i < havingAst.conditions().size(); i++) {
                HavingAst.SourceLocation location = i < havingAst.conditionLocations().size()
                        ? havingAst.conditionLocations().get(i)
                        : new HavingAst.SourceLocation(havingAst.line(), havingAst.column());
                validateExpression(havingAst.conditions().get(i), location.line(), location.column());
            }
        }

        private void validateExpression(TermAst expression, int line, int column) {
            visitExpressionTerms(expression, termAst -> {
                if (termAst instanceof LowerThanAst
                        || termAst instanceof LowerOrEqualThanAst
                        || termAst instanceof GreaterThanAst
                        || termAst instanceof GreaterOrEqualThanAst) {
                    BinaryConstraintAst comparison = (BinaryConstraintAst) termAst;
                    if (isPotentialIri(comparison.getLeftArgument())) {
                        addIriComparisonWarning(comparison, comparison.getLeftArgument(), line, column);
                    }
                    if (isPotentialIri(comparison.getRightArgument())) {
                        addIriComparisonWarning(comparison, comparison.getRightArgument(), line, column);
                    }
                }
            });
        }

        private void addIriComparisonWarning(
                BinaryConstraintAst comparison,
                TermAst operand,
                int line,
                int column) {
            result.add(buildAdvisoryDiagnostic(
                    operand.getName() + " is an IRI used with ordered comparison " + comparison.getName(),
                    operand.getName(),
                    line,
                    column));
        }
    }

}
