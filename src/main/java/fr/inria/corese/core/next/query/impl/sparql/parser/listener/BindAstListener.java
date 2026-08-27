package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlAstBuilder;
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
        VarAst variable = (VarAst) builder().variable(ctx.var_().getText());
        builder().addBind(new BindAst(expression, variable));
    }
}
