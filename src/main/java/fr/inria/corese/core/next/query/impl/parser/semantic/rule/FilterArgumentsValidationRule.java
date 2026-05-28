package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
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
        List<QueryDiagnostic> result = new ArrayList<>();
        List<PatternAst> filterPatternAst = queryAst.whereClause().patterns().stream().filter(patternAst -> patternAst instanceof FilterAst).toList();
        List<FilterAst> badFilters = filterPatternAst.stream().filter(patternAst -> {
           FilterAst filter = (FilterAst) patternAst;
           return checkFilterBoolean(filter);
        }).map(patternAst -> (FilterAst) patternAst).toList();
        badFilters.forEach(patternAst -> {
            result.add(this.buildIncorrectTypeDiagnostic(patternAst.operator().getName(), "FILTER", "boolean"));
        });
        return result;
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
                || filterAst.operator() instanceof VarAst
                || filterAst.operator() instanceof LiteralAst);
    }
}

