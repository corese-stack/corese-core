package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.ASTConstants;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupByAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SolutionModifierAstListener extends AbstractSparqlQueryAstListener {

    public SolutionModifierAstListener(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitGroupClause(SparqlParser.GroupClauseContext ctx) {
        List<TermAst> terms = new ArrayList<>(ctx.groupCondition().size());
        Set<String> expressionBoundVariables = new LinkedHashSet<>();
        Map<String, TermAst> expressionTerms = new LinkedHashMap<>();
        for (SparqlParser.GroupConditionContext gcc : ctx.groupCondition()) {
            TermAst expression = builder().termFromGroupCondition(gcc);
            terms.add(expression);
            if (gcc.expression() != null && gcc.var_() != null) {
                String aliasName = normalizeVariableName(gcc.var_().getText());
                expressionBoundVariables.add(aliasName);
                expressionTerms.put(aliasName, expression);
            }
        }
        queryBuilder().setGroupBy(new GroupByAst(terms, expressionBoundVariables, expressionTerms));
    }

    private String normalizeVariableName(String rawName) {
        if (rawName == null) {
            return "";
        }
        String trimmed = rawName.trim();
        if (trimmed.startsWith("?") || trimmed.startsWith("$")) {
            return trimmed.substring(1).trim();
        }
        return trimmed;
    }

    @Override
    public void exitLimitClause(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.LimitClauseContext ctx) {
        long limit = Long.parseLong(ctx.INTEGER().getText());
        queryBuilder().setLimit(limit);
    }

    @Override
    public void exitOffsetClause(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.OffsetClauseContext ctx) {
        long offset = Long.parseLong(ctx.INTEGER().getText());
        queryBuilder().setOffset(offset);
    }

    @Override
    public void exitOrderCondition(SparqlParser.OrderConditionContext ctx) {
        ASTConstants.OrderDirection direction = ASTConstants.OrderDirection.ASC;
        if(ctx.DESC() != null) {
            direction = ASTConstants.OrderDirection.DESC;
        }

        TermAst orderExpression;
        if(ctx.brackettedExpression() != null) {
            orderExpression = this.builder().termFromBrackettedExpression(ctx.brackettedExpression());
        } else if(ctx.constraint() != null) {
            orderExpression = builder().termFromConstraint(ctx.constraint());
        } else if(ctx.var_() != null) {
            orderExpression = builder().termFromVar(ctx.var_());
        } else {
            throw new QueryEvaluationException("Unexpected expression in ORDER BY clause " + ctx.getText());
        }
        this.queryBuilder().addOrderExpression(direction, orderExpression);
    }


}
