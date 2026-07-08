package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlUpdateAstBuilder;

/**
 * AST feature listener for the minimal {@code INSERT DATA} update operation.
 */
public class InsertDataRequestFeature extends AbstractSparqlRequestFeature {

    public InsertDataRequestFeature(SparqlUpdateAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitInsertData(SparqlParser.InsertDataContext ctx) {
        updateBuilder().addRequest(updateBuilder().insertDataToAst(ctx));
    }
}
