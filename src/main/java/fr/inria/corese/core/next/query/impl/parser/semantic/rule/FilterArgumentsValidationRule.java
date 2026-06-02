package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.data.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.AbstractAstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This rule checks the operators used in Filter asts
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

    /**
     * Check that the given term is either boolean expression, literal expression (that may return a boolean result) or a variable
     */
    private static boolean checkTermIsPotentialBoolean(TermAst termAst) {
        if(termAst instanceof BooleanExpressionAst) {
            return true;
        }
        if(termAst instanceof LiteralExpressionAst literalExpressionAst
                && ! ( literalExpressionAst instanceof XsdDateTimeExpressionAst
                || literalExpressionAst instanceof XsdDayTimeDurationExpressionAst
                || literalExpressionAst instanceof NumericExpressionAst
            )) { // Is a literal expression that could be a boolean, we cannot know
            return true;
        }
        if(termAst instanceof FunctionCallAst) { // Is a function call that could return a boolean, we cannot know
            return true;
        }
        if(termAst instanceof IfAst ifAst
                && checkTermIsPotentialBoolean(ifAst.thenExpr())
                && checkTermIsPotentialBoolean(ifAst.elseExpr())) { // Is a IF that returns potential booleans
            return true;
        }
        if(termAst instanceof LiteralAst(String lexical, String lang, String datatype)) {// is a literal that is a typed as a boolean or the string representation of one
                if(datatype != null) {
                    if(datatype.equals(XSD.xsdBoolean.getIRI().stringValue())) {
                        return true;
                    }
                } else {
                    return lexical.trim().equalsIgnoreCase("true") || lexical.trim().equalsIgnoreCase("false");
                }
        }
        if(termAst instanceof VarAst) { // Is a variable that can be resolved to a boolean
            return true;
        }

        return false;
    }

    private class FilterArgumentsValidationVisitor extends AbstractAstVisitor {
        private final List<QueryDiagnostic> result = new ArrayList<>();

        public List<QueryDiagnostic> getResult() {
            return result;
        }

        @Override
        public void visit(PatternAst patternAst) {
            if (patternAst instanceof FilterAst(TermAst operator) && !checkTermIsPotentialBoolean(operator)) {
                result.add(buildIncorrectTypeDiagnostic(operator.getName(), "FILTER", "boolean"));
            }
        }

        @Override
        public void visit(HavingAst havingAst) {
            for (TermAst condition : havingAst.conditions()) {
                if (!checkTermIsPotentialBoolean(condition)) {
                    result.add(buildIncorrectTypeDiagnostic(condition.getName(), "HAVING", "boolean"));
                }
            }
        }
    }
}

