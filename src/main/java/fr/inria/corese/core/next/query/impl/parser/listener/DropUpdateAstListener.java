package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlUpdateAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.DropRequestAst;

/**
 * AST feature listener for DROP SPARQL update query
 */
public class DropUpdateAstListener extends AbstractSparqlUpdateAstListener {
    public DropUpdateAstListener(SparqlUpdateAstBuilder builder) {
        super(builder);
    }

    @Override
    public void enterDrop(SparqlParser.DropContext ctx) {
        DropRequestAst dropRequestAst = this.updateBuilder().droptoAst(ctx);
        this.updateBuilder().addRequest(dropRequestAst);
    }
}