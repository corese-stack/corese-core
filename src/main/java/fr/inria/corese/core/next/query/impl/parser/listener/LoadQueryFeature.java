package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;

public class LoadQueryFeature extends AbstractSparqlFeature {

    public LoadQueryFeature(SparqlAstBuilder builder) {
        super(builder);
    }

    public void exitLoad(SparqlParser.LoadContext ctx) {
        this.builder().enterLoadQuery();
        this.builder().setSilentFlag(ctx.SILENT() != null);
        if(ctx.iriRef() != null) {
            this.builder().setSourceGraphIri((IriAst) this.builder().termFromIriRef(ctx.iriRef()));
        }
        if(ctx.graphRef() != null) {
            this.builder().setTargetGraphIri((IriAst) this.builder().termFromGraphRef(ctx.graphRef()));
        }
    }
}
