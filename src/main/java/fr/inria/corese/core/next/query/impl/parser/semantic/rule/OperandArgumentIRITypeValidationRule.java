package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import com.typesafe.sslconfig.ssl.LessThan;
import com.typesafe.sslconfig.ssl.LessThanOrEqual;
import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.AbstractAstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.corese.core.next.query.impl.parser.semantic.support.SemanticValidationUtils.checkTermIsPotentialIri;

/**
 * Check that the comparison operand do not compare IRIs (except for the different != operator)
 */
public class OperandArgumentIRITypeValidationRule extends AbstractSemanticValidationRule {

    private static final Logger logger = LoggerFactory.getLogger(OperandArgumentIRITypeValidationRule.class);

    @Override
    protected String getDiagnosticSource() {
        return OperandArgumentBooleanTypeValidationRule.class.getSimpleName();
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
            logger.debug(termAst.getName());
            if(termAst instanceof BinaryConstraintAst binaryConstraintAst) {
                logger.debug("{} {} {}", binaryConstraintAst.getName(), binaryConstraintAst.getLeftArgument().getName(), binaryConstraintAst.getRightArgument().getName());
                if(binaryConstraintAst instanceof LowerThanAst
                    || binaryConstraintAst instanceof LowerOrEqualThanAst
                    || binaryConstraintAst instanceof GreaterThanAst
                    || binaryConstraintAst instanceof GreaterOrEqualThanAst) {
                    logger.debug("{} {} {}", binaryConstraintAst.getName(), binaryConstraintAst.getLeftArgument().getName(), checkTermIsPotentialIri(binaryConstraintAst.getLeftArgument()));
                    if(checkTermIsPotentialIri(binaryConstraintAst.getLeftArgument())) {
                        result.add(buildIncorrectTypeDiagnostic(binaryConstraintAst.getLeftArgument().getName(), binaryConstraintAst.getName(), "not an IRI"));
                    }
                    logger.debug("{} {} {}", binaryConstraintAst.getName(), binaryConstraintAst.getRightArgument().getName(), checkTermIsPotentialIri(binaryConstraintAst.getRightArgument()));
                    if(checkTermIsPotentialIri(binaryConstraintAst.getRightArgument())) {
                        result.add(buildIncorrectTypeDiagnostic(binaryConstraintAst.getRightArgument().getName(), binaryConstraintAst.getName(), "not an IRI"));
                    }
                }
            }
        }
    }

}
