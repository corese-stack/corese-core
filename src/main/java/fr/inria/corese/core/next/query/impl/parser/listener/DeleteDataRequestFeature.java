package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlUpdateAstBuilder;

/**
 * AST feature listener for the minimal {@code DELETE DATA} update operation.
 */
public class DeleteDataRequestFeature extends AbstractSparqlRequestFeature {

    public DeleteDataRequestFeature(SparqlUpdateAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitDeleteData(SparqlParser.DeleteDataContext ctx) {
        updateBuilder().addRequest(updateBuilder().deleteDataToAst(ctx));
    }
}
