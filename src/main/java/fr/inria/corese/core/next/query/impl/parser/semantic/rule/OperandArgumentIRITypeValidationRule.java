package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.AbstractAstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.corese.core.next.query.impl.parser.semantic.support.SemanticValidationUtils.isPotentialIri;

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
        public void visit(TermAst termAst) {
            if (termAst instanceof BinaryConstraintAst binaryConstraintAst
                    && (binaryConstraintAst instanceof LowerThanAst
                    || binaryConstraintAst instanceof LowerOrEqualThanAst
                    || binaryConstraintAst instanceof GreaterThanAst
                    || binaryConstraintAst instanceof GreaterOrEqualThanAst)) {
                if (isPotentialIri(binaryConstraintAst.getLeftArgument())) {
                    result.add(buildIncorrectTypeDiagnostic(
                            binaryConstraintAst.getLeftArgument().getName(),
                            binaryConstraintAst.getName(),
                            "not an IRI"));
                }
                if (isPotentialIri(binaryConstraintAst.getRightArgument())) {
                    result.add(buildIncorrectTypeDiagnostic(
                            binaryConstraintAst.getRightArgument().getName(),
                            binaryConstraintAst.getName(),
                            "not an IRI"));
                }
            }
        }
    }

}
