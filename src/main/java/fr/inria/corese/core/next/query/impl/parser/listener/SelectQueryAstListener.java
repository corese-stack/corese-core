package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.parser.SparqlQueryAstBuilder;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.VariableScopeAnalyzer;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SPARQL SELECT query feature: sets query type and projection (SELECT * or SELECT ?v1 ?v2 ...).
 * <p>
 * In {@link #enterSelectQuery(SparqlParser.SelectQueryContext)} we:
 * 1. Call {@link SparqlQueryAstBuilder#enterSelectQuery()} to set query type.
 * 2. Extract the projection from the parse context (grammar: {@code (var_+ | '*')}) and call
 *    {@link SparqlQueryAstBuilder#setProjectionAll()}.
 * <p>
 * The WHERE clause is built by {@link BgpAstListener} (enter/exit GroupGraphPattern, TriplesBlock, addTriple).
 * <p>
 * Grammar {@code subSelect} (nested {@code SELECT} in a group) uses the same {@link SparqlAstBuilder} stack frames as a top-level SELECT.
 * <p>
 * SPARQL 1.1: only {@code SELECT} may appear as a subquery inside {@code { ... }} ({@code subSelect} rule).
 * Top-level {@code query} may be {@code SELECT}, {@code ASK}, {@code CONSTRUCT}, or {@code DESCRIBE}.
 * Nested {@code subSelect} uses the same {@link SparqlAstBuilder} stack frames as a top-level {@code SELECT}.
 */
public class SelectQueryAstListener extends AbstractSparqlAstListener implements QueryAstListener {

    private final VariableScopeAnalyzer variableScopeAnalyzer = new VariableScopeAnalyzer();

    public SelectQueryAstListener(SparqlQueryAstBuilder builder) {
        super(builder);
    }

    public SparqlQueryAstBuilder queryBuilder() {
        return (SparqlQueryAstBuilder) builder();
    }

    @Override
    public void enterSelectQuery(SparqlParser.SelectQueryContext ctx) {
        queryBuilder().enterSelectQuery();
        SparqlParser.SelectClauseContext selectClause = ctx.selectClause();
        if (selectClause.DISTINCT() != null) { queryBuilder().setDistinct(true); }
        if (selectClause.REDUCED() != null) { queryBuilder().setReduced(true); }

        extractProjection(selectClause);
    }

    @Override
    public void exitSelectQuery(SparqlParser.SelectQueryContext ctx) {
        queryBuilder().exitSelectQuery();
    }

    @Override
    public void enterSubSelect(SparqlParser.SubSelectContext ctx) {
        queryBuilder().enterSelectQuery();
        SparqlParser.SelectClauseContext selectClause = ctx.selectClause();
        if (selectClause.DISTINCT() != null) {
            queryBuilder().setDistinct(true);
        }
        if (selectClause.REDUCED() != null) {
            queryBuilder().setReduced(true);
        }
        extractProjection(selectClause);
    }

    @Override
    public void exitSubSelect(SparqlParser.SubSelectContext ctx) {
        queryBuilder().exitSelectQuery();
    }

    /**
     * Extracts SELECT * or SELECT ?v1 ?v2 ... (expr AS ?v3) ... from the parse context.
     * Grammar: {@code SELECT (DISTINCT | REDUCED)? (selectVar+ | '*')}
     * where {@code selectVar ::= var_ | '(' expression AS var_ ')'}
     */
    private void extractProjection(SparqlParser.SelectClauseContext ctx) {
        if (ctx.STAR() != null) {
            queryBuilder().setProjectionAll();
            return;
        }
        List<String> allVars = new ArrayList<>();
        List<String> expressionBoundVars = new ArrayList<>();
        Map<String, TermAst> expressionTerms = new LinkedHashMap<>();
        Map<String, Set<String>> expressionReferencedVariables = new LinkedHashMap<>();
        for (SparqlParser.SelectVarContext selectVar : ctx.selectVar()) {
            if (selectVar.expression() != null) {
                // (expr AS ?var) — introduces a new variable, not projected from WHERE
                String varName = selectVar.var_().getText();
                allVars.add(varName);
                expressionBoundVars.add(varName);
                TermAst expressionAst = builder().termFromExpression(selectVar.expression());
                expressionTerms.put(varName, expressionAst);
                expressionReferencedVariables.put(varName, variableScopeAnalyzer.collectReferencedVariables(expressionAst));
            } else if (selectVar.var_() != null) {
                allVars.add(selectVar.var_().getText());
            }
        }
        queryBuilder().setProjectionVariables(allVars, expressionBoundVars, expressionTerms, expressionReferencedVariables);
    }
}
