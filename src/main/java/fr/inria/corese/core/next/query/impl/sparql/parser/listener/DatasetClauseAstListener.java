package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;

public class DatasetClauseAstListener extends AbstractSparqlAstListener {
    public DatasetClauseAstListener(SparqlAstBuilder builder) {
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
