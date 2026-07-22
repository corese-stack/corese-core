package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlUpdateAstBuilder;
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
        AddRequestAst ast = this.updateBuilder().addtoAst(ctx);
        this.updateBuilder().addRequest(ast);
    }
}
