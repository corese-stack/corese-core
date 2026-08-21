package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlUpdateAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.ClearRequestAst;

/**
 * AST feature listener for CLEAR SPARQL update query
 */
public class ClearUpdateAstListener extends AbstractSparqlUpdateAstListener {
    public ClearUpdateAstListener(SparqlUpdateAstBuilder builder) {
        super(builder);
    }

    @Override
    public void enterClear(SparqlParser.ClearContext ctx) {
        ClearRequestAst clearRequestAst = this.updateBuilder().cleartoAst(ctx);
        this.updateBuilder().addRequest(clearRequestAst);
    }
}
