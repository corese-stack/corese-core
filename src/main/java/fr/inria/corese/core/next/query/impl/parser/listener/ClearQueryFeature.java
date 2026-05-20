package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;

/**
 * AST feature listener for CLEAR SPARQL update query
 */
public class ClearQueryFeature extends AbstractSparqlFeature {
    public ClearQueryFeature(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitClear(SparqlParser.ClearContext ctx) {
        this.builder().exitClearQuery();
        this.builder().setSilentFlag(ctx.SILENT() != null);
        if(ctx.graphRefAll() != null) {
            this.builder().setTargetGraphIri(this.builder().graphRefFromGraphRefAll(ctx.graphRefAll()));
        }
    }
}
