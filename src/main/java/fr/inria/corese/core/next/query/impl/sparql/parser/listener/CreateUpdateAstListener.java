package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlUpdateAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.CreateRequestAst;

/**
 * AST feature listener for the CREATE SPARQL update operation.
 */
public class CreateUpdateAstListener extends AbstractSparqlUpdateAstListener {

    public CreateUpdateAstListener(SparqlUpdateAstBuilder builder) {
        super(builder);
    }

    @Override
    public void enterCreate(SparqlParser.CreateContext ctx) {
        CreateRequestAst createRequestAst = this.updateBuilder().createToAst(ctx);
        this.updateBuilder().addRequest(createRequestAst);
    }
}
