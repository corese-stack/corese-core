package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.BindAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;

/**
 * SPARQL {@code BIND} feature
 */
public class BindAstListener extends AbstractSparqlAstListener {

    public BindAstListener(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void exitBind(SparqlParser.BindContext ctx) {
        TermAst expression = builder().termFromExpression(ctx.expression());
        VarAst variable = (VarAst) builder().var(ctx.var_().getText());
        builder().addBind(new BindAst(expression, variable));
    }
}
