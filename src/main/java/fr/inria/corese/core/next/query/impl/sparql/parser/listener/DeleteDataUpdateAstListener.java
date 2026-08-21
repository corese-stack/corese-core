package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlUpdateAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.DeleteDataRequestAst;

/**
 * AST feature listener for DELETE DATA SPARQL update query.
 */
public class DeleteDataUpdateAstListener extends AbstractSparqlUpdateAstListener {
    public DeleteDataUpdateAstListener(SparqlUpdateAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitDeleteData(SparqlParser.DeleteDataContext ctx) {
        DeleteDataRequestAst ast = this.updateBuilder().deleteDataToAst(ctx);
        this.updateBuilder().addRequest(ast);
    }
}
