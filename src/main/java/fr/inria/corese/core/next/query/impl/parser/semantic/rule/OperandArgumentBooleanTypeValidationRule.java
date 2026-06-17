package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.AbstractAstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.AndAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BinaryConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BooleanNotAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.OrAst;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.corese.core.next.query.impl.parser.semantic.support.SemanticValidationUtils.checkTermIsPotentialBoolean;

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
        public void visit(TermAst termAst) {
            if(termAst instanceof BooleanNotAst booleanNotAst) {
                if(! checkTermIsPotentialBoolean( booleanNotAst.argument())) {
                    result.add(buildIncorrectTypeDiagnostic(booleanNotAst.argument().getName(), booleanNotAst.getName(), "boolean"));
                }
            }
            if(termAst instanceof BinaryConstraintAst binaryConstraintAst) {
                if(binaryConstraintAst instanceof AndAst
                    || binaryConstraintAst instanceof OrAst) {
                    if(! checkTermIsPotentialBoolean( binaryConstraintAst.getLeftArgument())) {
                        result.add(buildIncorrectTypeDiagnostic(binaryConstraintAst.getLeftArgument().getName(), binaryConstraintAst.getName(), "boolean"));
                    }
                    if(! checkTermIsPotentialBoolean( binaryConstraintAst.getRightArgument())) {
                        result.add(buildIncorrectTypeDiagnostic(binaryConstraintAst.getRightArgument().getName(), binaryConstraintAst.getName(), "boolean"));
                    }
                }
            }
        }
    }
}
