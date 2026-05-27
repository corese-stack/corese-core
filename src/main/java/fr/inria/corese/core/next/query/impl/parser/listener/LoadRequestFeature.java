package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.parser.SparqlUpdateAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LoadRequestAst;

/**
 * AST feature listener for LOAD SPARQL update query
 */
public class LoadRequestFeature extends AbstractSparqlRequestFeature {

    public LoadRequestFeature(SparqlUpdateAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitLoad(SparqlParser.LoadContext ctx) {
        LoadRequestAst loadRequestAst = this.updateBuilder().loadToAst(ctx);
        this.updateBuilder().addRequest(loadRequestAst);
    }
}
