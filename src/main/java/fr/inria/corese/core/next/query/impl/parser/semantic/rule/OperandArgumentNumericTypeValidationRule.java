package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.AbstractAstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.corese.core.next.query.impl.parser.semantic.support.SemanticValidationUtils.checkTermIsPotentialNumeric;

public final class OperandArgumentNumericTypeValidationRule extends AbstractSemanticValidationRule {

    @Override
    protected String getDiagnosticSource() {
        return OperandArgumentNumericTypeValidationRule.class.getSimpleName();
    }

    @Override
    public List<QueryDiagnostic> validate(QueryAst queryAst) {
        OperandArgumentTypeVisitor visitor = new OperandArgumentTypeVisitor();
        queryAst.accept(visitor);
        return visitor.getResult();
    }

    private class OperandArgumentTypeVisitor extends AbstractAstVisitor {
        private final List<QueryDiagnostic> result = new ArrayList<>();

        public List<QueryDiagnostic> getResult() {
            return result;
        }

        ;

        @Override
        public void visit(TermAst termAst) {
            if (termAst instanceof UnaryConstraintAst unaryConstraintAst) {
                if (
                        (unaryConstraintAst instanceof AbsAst
                                || unaryConstraintAst instanceof CeilAst
                                || unaryConstraintAst instanceof FloorAst
                                || unaryConstraintAst instanceof RoundAst
                                || unaryConstraintAst instanceof UnaryMinusAst
                                || unaryConstraintAst instanceof UnaryPlusAst)
                                && !checkTermIsPotentialNumeric(unaryConstraintAst.argument())) {
                    result.add(buildIncorrectTypeDiagnostic(unaryConstraintAst.argument().getName(), unaryConstraintAst.getName(), "numeric"));
                }
            }

            if (termAst instanceof BinaryConstraintAst binaryConstraintAst) { // Numeric only operands
                if (binaryConstraintAst instanceof AddAst
                        || binaryConstraintAst instanceof DivideAst
                        || binaryConstraintAst instanceof MultiplyAst
                        || binaryConstraintAst instanceof SubtractAst
                ) {
                    if (!checkTermIsPotentialNumeric(binaryConstraintAst.getLeftArgument())) {
                        result.add(buildIncorrectTypeDiagnostic(binaryConstraintAst.getLeftArgument().getName(), binaryConstraintAst.getName(), "numeric"));
                    }
                    if (!checkTermIsPotentialNumeric(binaryConstraintAst.getRightArgument())) {
                        result.add(buildIncorrectTypeDiagnostic(binaryConstraintAst.getRightArgument().getName(), binaryConstraintAst.getName(), "numeric"));
                    }
                }
            }
        }
    }
}
