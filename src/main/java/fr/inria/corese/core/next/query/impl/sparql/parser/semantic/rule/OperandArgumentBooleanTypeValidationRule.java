package fr.inria.corese.core.next.query.impl.sparql.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.HavingAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.PatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.visitor.AbstractAstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.AndAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BinaryConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BooleanNotAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.OrAst;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.SemanticValidationUtils.isPotentialBooleanCompatible;
import static fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.SemanticValidationUtils.isSuspiciousEBVCoercion;
import static fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.SemanticValidationUtils.visitExpressionTerms;


public class OperandArgumentBooleanTypeValidationRule extends AbstractSemanticValidationRule {
    @Override
    protected String getDiagnosticSource() {
        return OperandArgumentBooleanTypeValidationRule.class.getSimpleName();
    }

    @Override
    public List<QueryDiagnostic> validate(QueryAst queryAst) {
        OperandBooleanArgumentTypeVisitor visitor = new OperandBooleanArgumentTypeVisitor();
        queryAst.accept(visitor);
        return visitor.getResult();
    }

    private class OperandBooleanArgumentTypeVisitor extends AbstractAstVisitor {
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
                if (termAst instanceof BooleanNotAst booleanNotAst) {
                    validateBooleanOperand(booleanNotAst.argument(), booleanNotAst.getName(), line, column);
                } else if (termAst instanceof AndAst andAst) {
                    validateBooleanOperands(andAst, line, column);
                } else if (termAst instanceof OrAst orAst) {
                    validateBooleanOperands(orAst, line, column);
                }
            });
        }

        private void validateBooleanOperands(BinaryConstraintAst expression, int line, int column) {
            validateBooleanOperand(expression.getLeftArgument(), expression.getName(), line, column);
            validateBooleanOperand(expression.getRightArgument(), expression.getName(), line, column);
        }

        private void validateBooleanOperand(TermAst operand, String operator, int line, int column) {
            if (!isPotentialBooleanCompatible(operand) || isSuspiciousEBVCoercion(operand)) {
                result.add(buildAdvisoryDiagnostic(
                        operand.getName() + " used in " + operator
                                + " may require implicit effective boolean value conversion",
                        operand.getName(),
                        line,
                        column));
            }
        }
    }
}
