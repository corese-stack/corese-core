package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.impl.parser.antlr.SparqlParserBaseListener;
import fr.inria.corese.core.next.impl.parser.antlr.SparqlParserListener;

import java.util.Collections;
import java.util.List;

/**
 * Dispatches the supported ANTLR SPARQL events to a fixed list of delegates.
 *
 * <p>The dispatcher owns no AST-building state. Query-form, graph-pattern,
 * expression, modifier, prologue, and update events remain implemented by the
 * specialized listeners in the {@code listener} subpackage.</p>
 */
final class SparqlListenerDispatcher extends SparqlParserBaseListener {

    private final List<? extends SparqlParserListener> delegates;

    SparqlListenerDispatcher(List<? extends SparqlParserListener> delegates) {
        this.delegates = delegates != null && !delegates.isEmpty()
                ? List.copyOf(delegates)
                : Collections.emptyList();
    }

    // ---------- QUERY ROOT ------------------

    @Override
    public void enterSelectQuery(SparqlParser.SelectQueryContext ctx) {
        for(var d : delegates) d.enterSelectQuery(ctx);
    }

    @Override
    public void exitSelectQuery(SparqlParser.SelectQueryContext ctx) {
        for (var d : delegates) d.exitSelectQuery(ctx);
    }

    @Override
    public void enterSubSelect(SparqlParser.SubSelectContext ctx) {
        for (var d : delegates) d.enterSubSelect(ctx);
    }

    @Override
    public void exitSubSelect(SparqlParser.SubSelectContext ctx) {
        for (var d : delegates) d.exitSubSelect(ctx);
    }

    @Override
    public void enterAskQuery(SparqlParser.AskQueryContext ctx) {
        for(var d : delegates) d.enterAskQuery(ctx);
    }

    @Override
    public void enterConstructQuery(SparqlParser.ConstructQueryContext ctx) {
        for (var d : delegates) d.enterConstructQuery(ctx);
    }

    @Override
    public void exitConstructQuery(SparqlParser.ConstructQueryContext ctx) {
        for (var d : delegates) d.exitConstructQuery(ctx);
    }

    @Override
    public void enterConstructTemplate(SparqlParser.ConstructTemplateContext ctx) {
        for (var d : delegates) d.enterConstructTemplate(ctx);
    }

    @Override
    public void exitConstructTemplate(SparqlParser.ConstructTemplateContext ctx) {
        for (var d : delegates) d.exitConstructTemplate(ctx);
    }

    @Override
    public void enterLoad(SparqlParser.LoadContext ctx) {
        for (var d : delegates) d.enterLoad(ctx);
    }

    @Override
    public void exitLoad(SparqlParser.LoadContext ctx) {
        for (var d : delegates) d.exitLoad(ctx);
    }

    @Override
    public void enterClear(SparqlParser.ClearContext ctx) {
        for (var d : delegates) d.enterClear(ctx);
    }

    @Override
    public void exitClear(SparqlParser.ClearContext ctx) {
        for (var d : delegates) d.exitClear(ctx);
    }

    @Override
    public void enterWhereClause(SparqlParser.WhereClauseContext ctx) {
        for (var d : delegates) d.enterWhereClause(ctx);
    }

    // ---------- GROUP GRAPH PATTERN ----------
    @Override
    public void enterGroupGraphPattern(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.GroupGraphPatternContext ctx) {
        for (var d : delegates) d.enterGroupGraphPattern(ctx);
    }

    @Override
    public void exitGroupGraphPattern(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.GroupGraphPatternContext ctx) {
        for (var d : delegates) d.exitGroupGraphPattern(ctx);
    }

    // ---------- TRIPLES BLOCK (BGP boundary) ----------
    @Override
    public void enterTriplesBlock(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.TriplesBlockContext ctx) {
        for (var d : delegates) d.enterTriplesBlock(ctx);
    }

    @Override
    public void exitTriplesBlock(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.TriplesBlockContext ctx) {
        for (var d : delegates) d.exitTriplesBlock(ctx);
    }

    // ---------- TRIPLES (subject + property list) ----------
    @Override
    public void exitTriplesSameSubject(SparqlParser.TriplesSameSubjectContext ctx) {
        for (var d : delegates) d.exitTriplesSameSubject(ctx);
    }
    @Override
    public void exitTriplesSameSubjectPath(SparqlParser.TriplesSameSubjectPathContext ctx) {
        for (var d : delegates) d.exitTriplesSameSubjectPath(ctx);
    }
    // ---------- OPTIONAL ----------
    @Override
    public void enterOptionalGraphPattern(SparqlParser.OptionalGraphPatternContext ctx) {
        for (var d : delegates) d.enterOptionalGraphPattern(ctx);
    }

    @Override
    public void exitOptionalGraphPattern(SparqlParser.OptionalGraphPatternContext ctx) {
        for (var d : delegates) d.exitOptionalGraphPattern(ctx);
    }

    // ---------- MINUS ----------
    @Override
    public void enterMinusGraphPattern(SparqlParser.MinusGraphPatternContext ctx) {
        for (var d : delegates) d.enterMinusGraphPattern(ctx);
    }

    @Override
    public void exitMinusGraphPattern(SparqlParser.MinusGraphPatternContext ctx) {
        for (var d : delegates) d.exitMinusGraphPattern(ctx);
    }

    // ---------- SERVICE ----------

    @Override
    public void enterServiceGraphPattern(SparqlParser.ServiceGraphPatternContext ctx) {
        for (var d : delegates) d.enterServiceGraphPattern(ctx);
    }

    @Override
    public void exitServiceGraphPattern(SparqlParser.ServiceGraphPatternContext ctx) {
        for (var d : delegates) d.exitServiceGraphPattern(ctx);
    }

    // ---------- FILTER -----------

    @Override
    public void enterFilter_(SparqlParser.Filter_Context ctx) {
        for(var d : delegates) d.enterFilter_(ctx);
    }

    @Override
    public void exitFilter_(SparqlParser.Filter_Context ctx) {
        for(var d : delegates) d.exitFilter_(ctx);
    }

    // ---------- SOLUTION MODIFIER (GROUP BY / LIMIT / OFFSET / ORDER BY) ----------
    @Override
    public void exitGroupClause(SparqlParser.GroupClauseContext ctx) {
        for (var d : delegates) {
            d.exitGroupClause(ctx);
        }
    }

    // ---------- SOLUTION MODIFIER (HAVING / ORDER BY / LIMIT / OFFSET) ----------
    @Override
    public void exitHavingClause(SparqlParser.HavingClauseContext ctx) {
        for (var d : delegates) d.exitHavingClause(ctx);
    }

    @Override
    public void exitLimitClause(SparqlParser.LimitClauseContext ctx) {
        for (var d : delegates) d.exitLimitClause(ctx);
    }

    @Override
    public void exitOffsetClause(SparqlParser.OffsetClauseContext ctx) {
        for (var d : delegates) d.exitOffsetClause(ctx);
    }

    @Override
    public void exitOrderCondition(SparqlParser.OrderConditionContext ctx) {
        for (var d : delegates) d.exitOrderCondition(ctx);
    }

    /**
     * Forwards {@code enterGroupOrUnionGraphPattern} events to all registered delegates.
     *
     * @param ctx the ANTLR parse context for {@code groupOrUnionGraphPattern}
     */
    @Override
    public void enterGroupOrUnionGraphPattern(SparqlParser.GroupOrUnionGraphPatternContext ctx) {
        for (var d : delegates) d.enterGroupOrUnionGraphPattern(ctx);
    }

    /**
     * Forwards {@code exitGroupOrUnionGraphPattern} events to all registered delegates.
     *
     * @param ctx the ANTLR parse context for {@code groupOrUnionGraphPattern}
     */
    @Override
    public void exitGroupOrUnionGraphPattern(SparqlParser.GroupOrUnionGraphPatternContext ctx) {
        for (var d : delegates) d.exitGroupOrUnionGraphPattern(ctx);
    }

    @Override
    public void enterDescribeQuery(SparqlParser.DescribeQueryContext ctx) {
        for (var d : delegates) d.enterDescribeQuery(ctx);
    }

    @Override
    public void exitDescribeQuery(SparqlParser.DescribeQueryContext ctx) {
        for (var d : delegates) d.exitDescribeQuery(ctx);
    }

    @Override
    public void exitDefaultGraphClause(SparqlParser.DefaultGraphClauseContext ctx) {
        for (var d : delegates) d.exitDefaultGraphClause(ctx);
    }

    @Override
    public void exitNamedGraphClause(SparqlParser.NamedGraphClauseContext ctx) {
        for (var d : delegates) d.exitNamedGraphClause(ctx);
    }

    @Override public void enterBaseDecl(SparqlParser.BaseDeclContext ctx) {
        for (var d : delegates) d.enterBaseDecl(ctx);
    }

    @Override public void exitBaseDecl(SparqlParser.BaseDeclContext ctx) {
        for (var d : delegates) d.exitBaseDecl(ctx);
    }

    @Override public void enterPrefixDecl(SparqlParser.PrefixDeclContext ctx) {
        for (var d : delegates) d.enterPrefixDecl(ctx);
    }

    @Override public void exitPrefixDecl(SparqlParser.PrefixDeclContext ctx) {
        for (var d : delegates) d.exitPrefixDecl(ctx);
    }

    @Override
    public void enterExistsFunc(SparqlParser.ExistsFuncContext ctx) {
        for (var d : delegates) d.enterExistsFunc(ctx);
    }

    @Override
    public void enterNotExistsFunc(SparqlParser.NotExistsFuncContext ctx) {
        for (var d : delegates) d.enterNotExistsFunc(ctx);
    }

    @Override
    public void exitBind(SparqlParser.BindContext ctx) {
        for (var d : delegates) d.exitBind(ctx);
    }

    @Override
    public void enterValuesClause(SparqlParser.ValuesClauseContext ctx) {
        for (var d : delegates) d.enterValuesClause(ctx);
    }

    @Override
    public void exitValuesClause(SparqlParser.ValuesClauseContext ctx) {
        for (var d : delegates) d.exitValuesClause(ctx);
    }

    @Override
    public void enterInlineData(SparqlParser.InlineDataContext ctx) {
        for (var d : delegates) d.enterInlineData(ctx);
    }

    @Override
    public void exitInlineData(SparqlParser.InlineDataContext ctx) {
        for (var d : delegates) d.exitInlineData(ctx);
    }

    @Override
    public void enterCreate(SparqlParser.CreateContext ctx) {
        for (var d : delegates) d.enterCreate(ctx);
    }

    @Override
    public void exitCreate(SparqlParser.CreateContext ctx) {
        for (var d : delegates) d.exitCreate(ctx);
    }

    @Override
    public void enterDrop(SparqlParser.DropContext ctx) {
        for (var d : delegates) d.enterDrop(ctx);
    }

    @Override
    public void exitDrop(SparqlParser.DropContext ctx) {
        for (var d : delegates) d.exitDrop(ctx);
    }

    @Override
    public void enterAdd(SparqlParser.AddContext ctx) {
        for (var d : delegates) d.enterAdd(ctx);
    }

    @Override
    public void exitAdd(SparqlParser.AddContext ctx) {
        for (var d : delegates) d.exitAdd(ctx);
    }
}
