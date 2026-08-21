package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlUpdateAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.LoadRequestAst;

/**
 * AST feature listener for LOAD SPARQL update query
 */
public class LoadUpdateAstListener extends AbstractSparqlUpdateAstListener {

    public LoadUpdateAstListener(SparqlUpdateAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitLoad(SparqlParser.LoadContext ctx) {
        LoadRequestAst loadRequestAst = this.updateBuilder().loadToAst(ctx);
        this.updateBuilder().addRequest(loadRequestAst);
    }
}
