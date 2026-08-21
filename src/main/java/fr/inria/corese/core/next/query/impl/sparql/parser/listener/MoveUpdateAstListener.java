package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlUpdateAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.MoveRequestAst;

/**
 * AST feature listener for MOVE SPARQL update query.
 */
public class MoveUpdateAstListener extends AbstractSparqlUpdateAstListener {
    public MoveUpdateAstListener(SparqlUpdateAstBuilder builder) {
        super(builder);
    }

    @Override
    public void enterMove(SparqlParser.MoveContext ctx) {
        MoveRequestAst ast = this.updateBuilder().moveToAst(ctx);
        this.updateBuilder().addRequest(ast);
    }
}
