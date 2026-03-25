package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;

public class DatasetClauseFeature extends AbstractSparqlFeature {
    public DatasetClauseFeature(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitDefaultGraphClause(SparqlParser.DefaultGraphClauseContext ctx) {
        builder().addFromGraph((IriAst) builder().termFromIriRef(ctx.sourceSelector().iriRef()));
    }

    @Override
    public void exitNamedGraphClause(SparqlParser.NamedGraphClauseContext ctx) {
        builder().addFromNamedGraph((IriAst) builder().termFromIriRef(ctx.sourceSelector().iriRef()));
    }
}
