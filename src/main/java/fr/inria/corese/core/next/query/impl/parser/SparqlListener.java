package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.impl.parser.antlr.SparqlParserBaseListener;
import fr.inria.corese.core.next.impl.parser.antlr.SparqlParserListener;

import java.util.Collections;
import java.util.List;

/**
 * SPARQL listener multiplexer: forwards to delegates only the events needed for
 * triple patterns (?s ?p ?o) and BGPs (GroupGraphPattern, TriplesBlock, TriplesSameSubject).
 * Additional rules can be added later if needed.
 */
public final class SparqlListener extends SparqlParserBaseListener {

    private final List<? extends SparqlParserListener> delegates;

    public SparqlListener(List<? extends SparqlParserListener> delegates) {
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
    public void enterAskQuery(SparqlParser.AskQueryContext ctx) {
        for(var d : delegates) d.enterAskQuery(ctx);
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
    public void exitTriplesSameSubject(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.TriplesSameSubjectContext ctx) {
        for (var d : delegates) d.exitTriplesSameSubject(ctx);
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
}
