package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlUpdateAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.CopyRequestAst;

/**
 * AST feature listener for COPY SPARQL update query.
 */
public class CopyUpdateAstListener extends AbstractSparqlUpdateAstListener {
    public CopyUpdateAstListener(SparqlUpdateAstBuilder builder) {
        super(builder);
    }

    @Override
    public void enterCopy(SparqlParser.CopyContext ctx) {
        CopyRequestAst ast = this.updateBuilder().copytoAst(ctx);
        this.updateBuilder().addRequest(ast);
    }
}