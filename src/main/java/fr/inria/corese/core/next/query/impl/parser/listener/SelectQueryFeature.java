package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * SPARQL SELECT query feature: sets query type and projection (SELECT * or SELECT ?v1 ?v2 ...).
 * <p>
 * In {@link #enterSelectQuery(SparqlParser.SelectQueryContext)} we:
 * 1. Call {@link SparqlAstBuilder#enterSelectQuery()} to set query type.
 * 2. Extract the projection from the parse context (grammar: {@code (var_+ | '*')}) and call
 *    {@link SparqlAstBuilder#setProjectionAll()} or {@link SparqlAstBuilder#setProjectionVariables(List)}.
 * <p>
 * The WHERE clause is built by {@link BgpFeature} (enter/exit GroupGraphPattern, TriplesBlock, addTriple).
 * At {@link SparqlAstBuilder#getResult()}, the builder produces a {@link fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst}
 * with both {@link fr.inria.corese.core.next.query.impl.sparql.ast.ProjectionAst} and the WHERE group.
 */
public class SelectQueryFeature extends AbstractSparqlFeature {

    public SelectQueryFeature(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void enterSelectQuery(SparqlParser.SelectQueryContext ctx) {
        builder().enterSelectQuery();
        SparqlParser.SelectClauseContext selectClause = ctx.selectClause();
        if (selectClause.DISTINCT() != null) { builder().setDistinct(true); }
        if (selectClause.REDUCED() != null) { builder().setReduced(true); }

        extractProjection(selectClause);
    }

    @Override
    public void exitSelectQuery(SparqlParser.SelectQueryContext ctx) {
        builder().exitSelectQuery();
    }

    /**
     * Extracts SELECT * or SELECT ?v1 ?v2 ... from the parse context.
     * Grammar: {@code SELECT (DISTINCT | REDUCED)? (var_+ | '*') ...}
     */
    private void extractProjection(SparqlParser.SelectClauseContext ctx) {
        if (ctx.STAR() != null) {
            builder().setProjectionAll();
            return;
        }
        List<String> vars = new ArrayList<>();
        for (SparqlParser.SelectVarContext selectVar : ctx.selectVar()) {
            if (selectVar.var_() != null) {
                vars.add(selectVar.var_().getText());
            }
        }
        builder().setProjectionVariables(vars);
    }
}