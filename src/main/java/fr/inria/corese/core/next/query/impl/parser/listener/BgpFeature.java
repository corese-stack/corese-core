package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;

import java.util.ArrayList;
import java.util.List;


/**
 * SPARQL 1.0 feature: build Triple patterns + BGPs + OPTIONAL
 
 * Grammar hooks used:
 * - GroupGraphPattern: { ... }  -> builder.enterGroup()/exitGroup()
 * - TriplesBlock: BGP block      -> builder.enterBgp()/exitBgp()
 * - TriplesSameSubject: produce actual triples -> builder.addTriple(s,p,o)
 * - OptionalGraphPattern: OPTIONAL { ... } -> builder.enterOptional()/exitOptional()
 */
public class BgpFeature extends AbstractSparqlFeature {

    public BgpFeature(SparqlAstBuilder builder) {
        super(builder);
    }

    // ------------ GROUP { .... } ------------

    @Override
    public void enterGroupGraphPattern(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.GroupGraphPatternContext ctx) {
        builder().enterGroup();
    }

    @Override
    public void exitGroupGraphPattern(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.GroupGraphPatternContext ctx) {
        builder().exitGroup();
    }

    // -------- OPTIONAL { ... } --------
    @Override
    public void enterOptionalGraphPattern(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.OptionalGraphPatternContext ctx) {
        builder().enterOptional();
    }

    @Override
    public void exitOptionalGraphPattern(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.OptionalGraphPatternContext ctx) {
        builder().exitOptional();
    }

    // -------- BGP (TriplesBlock) --------
    @Override
    public void enterTriplesBlock(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.TriplesBlockContext ctx) {
        builder().enterBgp();
    }

    @Override
    public void exitTriplesBlock(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.TriplesBlockContext ctx) {
        builder().exitBgp();
    }

    // -------- TRIPLE PATTERNS (?s ?p ?o) --------
    @Override
    public void exitTriplesSameSubject(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.TriplesSameSubjectContext ctx) {
        // subject + propertyListNotEmpty
        TermAst s = termFromVarOrTerm(ctx.varOrTerm());
        var pl = ctx.propertyListNotEmpty();
        if (pl == null) return;
        for (int i = 0; i < pl.verb().size(); i++) {
            TermAst p = termFromVerb(pl.verb(i));
            List<TermAst> objects = termListFromObjectList(pl.objectList(i));
            for (TermAst o : objects) { builder().addTriple(s, p, o); }
        }
    }
}
