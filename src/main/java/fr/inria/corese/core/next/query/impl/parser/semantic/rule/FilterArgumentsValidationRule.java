package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

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
     * Checks that the operators used in FILTER and HAVING are either boolean expression, literal expression (that may return a boolean result or variables)
     */
    private static boolean checkFilterBoolean(FilterAst filterAst) {
        return !(filterAst.operator() instanceof BooleanExpressionAst
                || (filterAst.operator() instanceof LiteralExpressionAst
                        && ! (filterAst.operator() instanceof XsdDateTimeExpressionAst
                            || filterAst.operator() instanceof XsdDayTimeDurationExpressionAst
                            || filterAst.operator() instanceof NumericExpressionAst)
                    )
                || filterAst.operator() instanceof FunctionCallAst
                || filterAst.operator() instanceof IfAst
                || filterAst.operator() instanceof VarAst
                || filterAst.operator() instanceof LiteralAst);
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

