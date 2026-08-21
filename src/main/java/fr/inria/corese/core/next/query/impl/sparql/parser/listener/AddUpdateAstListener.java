package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlUpdateAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.AddRequestAst;

/**
 * AST feature listener for ADD SPARQL update query.
 */
public class AddUpdateAstListener extends AbstractSparqlUpdateAstListener {
    public AddUpdateAstListener(SparqlUpdateAstBuilder builder) {
        super(builder);
    }

    @Override
    public void enterAdd(SparqlParser.AddContext ctx) {
        AddRequestAst ast = this.updateBuilder().addToAst(ctx);
        this.updateBuilder().addRequest(ast);
    }
}
