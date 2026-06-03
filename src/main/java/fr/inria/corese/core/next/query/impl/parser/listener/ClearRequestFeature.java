package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlUpdateAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.ClearRequestAst;

/**
 * AST feature listener for CLEAR SPARQL update query
 */
public class ClearRequestFeature extends AbstractSparqlRequestFeature {
    public ClearRequestFeature(SparqlUpdateAstBuilder builder) {
        super(builder);
    }

    @Override
    public void enterClear(SparqlParser.ClearContext ctx) {
        ClearRequestAst clearRequestAst = this.updateBuilder().cleartoAst(ctx);
        this.updateBuilder().addRequest(clearRequestAst);
    }
}
