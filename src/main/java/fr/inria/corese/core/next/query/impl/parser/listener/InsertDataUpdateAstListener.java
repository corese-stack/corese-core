package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlUpdateAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.InsertDataRequestAst;

/**
 * AST feature listener for INSERT DATA SPARQL update query.
 */
public class InsertDataUpdateAstListener extends AbstractSparqlUpdateAstListener {
    public InsertDataUpdateAstListener(SparqlUpdateAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitInsertData(SparqlParser.InsertDataContext ctx) {
        InsertDataRequestAst ast = this.updateBuilder().insertDataToAst(ctx);
        this.updateBuilder().addRequest(ast);
    }
}
