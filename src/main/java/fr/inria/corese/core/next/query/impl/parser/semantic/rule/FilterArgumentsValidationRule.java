package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.AbstractAstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static fr.inria.corese.core.next.query.impl.parser.semantic.support.SemanticValidationUtils.checkTermIsPotentialBoolean;

/**
 * Validates that FILTER and HAVING expressions are compatible with SPARQL
 * effective boolean value evaluation.
 */
public final class FilterArgumentsValidationRule extends AbstractSemanticValidationRule {

    private static final String BOOLEAN_DATATYPE = XSD.xsdBoolean.getIRI().stringValue();
    private static final String STRING_DATATYPE = XSD.xsdString.getIRI().stringValue();
    private static final Set<String> NUMERIC_DATATYPES = Set.of(
            XSD.xsdInteger.getIRI().stringValue(),
            XSD.xsdNonNegativeInteger.getIRI().stringValue(),
            XSD.xsdNonPositiveInteger.getIRI().stringValue(),
            XSD.xsdPositiveInteger.getIRI().stringValue(),
            XSD.xsdNegativeInteger.getIRI().stringValue(),
            XSD.xsdInt.getIRI().stringValue(),
            XSD.xsdUnsignedInt.getIRI().stringValue(),
            XSD.xsdLong.getIRI().stringValue(),
            XSD.xsdUnsignedLong.getIRI().stringValue(),
            XSD.xsdDecimal.getIRI().stringValue(),
            XSD.xsdShort.getIRI().stringValue(),
            XSD.xsdUnsignedShort.getIRI().stringValue(),
            XSD.xsdByte.getIRI().stringValue(),
            XSD.xsdUnsignedByte.getIRI().stringValue(),
            XSD.xsdFloat.getIRI().stringValue(),
            XSD.xsdDouble.getIRI().stringValue());

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

    /**
     * Checks whether the given term is statically compatible with SPARQL
     * effective boolean value evaluation.
     */
    private static boolean isPotentiallyEbvCompatible(TermAst termAst) {
        if (termAst instanceof BooleanExpressionAst
                || termAst instanceof NumericExpressionAst
                || termAst instanceof VarAst
                || termAst instanceof FunctionCallAst
                || termAst instanceof UnlimitedArgumentsFunctionAst) {
            return true;
        }

        if (termAst instanceof LiteralAst literalAst) {
            return isEbvCompatibleLiteral(literalAst);
        }

        if (termAst instanceof IfAst(TermAst condition, TermAst thenExpr, TermAst elseExpr)) {
            return isPotentiallyEbvCompatible(condition)
                    && isPotentiallyEbvCompatible(thenExpr)
                    && isPotentiallyEbvCompatible(elseExpr);
        }

        if (termAst instanceof LiteralExpressionAst literalExpressionAst) {
            return !(literalExpressionAst instanceof XsdDateTimeExpressionAst
                    || literalExpressionAst instanceof XsdDayTimeDurationExpressionAst);
        }

        return false;
    }

    private static boolean isEbvCompatibleLiteral(LiteralAst literalAst) {
        if (literalAst.lang() != null && !literalAst.lang().isBlank()) {
            return true;
        }

        String datatype = literalAst.datatype();
        if (datatype == null) {
            return true;
        }

        return BOOLEAN_DATATYPE.equals(datatype)
                || STRING_DATATYPE.equals(datatype)
                || NUMERIC_DATATYPES.contains(datatype);
    }

    private class FilterArgumentsValidationVisitor extends AbstractAstVisitor {
        private final List<QueryDiagnostic> result = new ArrayList<>();

        public List<QueryDiagnostic> getResult() {
            return result;
        }

        @Override
        public void visit(PatternAst patternAst) {
            if (patternAst instanceof FilterAst(TermAst operator) && !isPotentiallyEbvCompatible(operator)) {
                result.add(buildIncorrectTypeDiagnostic(operator.getName(), "FILTER", "boolean"));
            }
        }

        @Override
        public void visit(HavingAst havingAst) {
            for (TermAst condition : havingAst.conditions()) {
                if (!isPotentiallyEbvCompatible(condition)) {
                    result.add(buildIncorrectTypeDiagnostic(condition.getName(), "HAVING", "boolean"));
                }
            }
        }
    }

    private class FilterArgumentsValidationVisitor extends AbstractAstVisitor {
        private final List<QueryDiagnostic> result = new ArrayList<>();

        public List<QueryDiagnostic> getResult() {
            return result;
        };
        public void visit(PatternAst patternAst) {
            if(patternAst instanceof FilterAst filterAst && checkFilterBoolean(filterAst)) {
                result.add(buildIncorrectTypeDiagnostic(filterAst.operator().getName(), "FILTER", "boolean"));
            }
        }
    }
}
