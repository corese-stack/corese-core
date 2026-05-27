package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Parses {@code havingClause} ({@code HAVING havingCondition+}) into {@link fr.inria.corese.core.next.query.impl.sparql.ast.HavingAst}
 * conditions on the active {@link SparqlAstBuilder} SELECT frame (or top-level modifier lists for ASK / CONSTRUCT / DESCRIBE).
 */
public class HavingFeature extends AbstractSparqlQueryFeature {

    public HavingFeature(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitHavingClause(SparqlParser.HavingClauseContext ctx) {
        for (SparqlParser.HavingConditionContext condition : ctx.havingCondition()) {
            queryBuilder().addHavingCondition(havingConditionToTerm(condition));
        }
    }

    private TermAst havingConditionToTerm(SparqlParser.HavingConditionContext ctx) {
        if (ctx.constraint() != null) {
            return builder().termFromConstraint(ctx.constraint());
        }
        throw new QueryEvaluationException("Unsupported HAVING condition: " + ctx.getText());
    }
}
