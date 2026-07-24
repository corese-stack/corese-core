package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlUpdateAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.DeleteWhereRequestAst;

/**
 * AST feature listener for DELETE WHERE SPARQL update query.
 */
public class DeleteWhereUpdateAstListener extends AbstractSparqlUpdateAstListener {
    public DeleteWhereUpdateAstListener(SparqlUpdateAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitDeleteWhere(SparqlParser.DeleteWhereContext ctx) {
        DeleteWhereRequestAst ast = this.updateBuilder().deleteWhereToAst(ctx);
        this.updateBuilder().addRequest(ast);
    }
}
