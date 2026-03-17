package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;

public class SolutionModifierFeature extends AbstractSparqlFeature {

    private final SparqlAstBuilder builder;

    public SolutionModifierFeature(SparqlAstBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void exitLimitClause(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.LimitClauseContext ctx) {
        long limit = Long.parseLong(ctx.INTEGER().getText());
        builder.setLimit(limit);
    }

    @Override
    public void exitOffsetClause(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.OffsetClauseContext ctx) {
        long offset = Long.parseLong(ctx.INTEGER().getText());
        builder.setOffset(offset);
    }

}
