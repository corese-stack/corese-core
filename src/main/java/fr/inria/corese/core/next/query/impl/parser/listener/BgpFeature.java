package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;

import java.util.ArrayList;
import java.util.List;


/**
 * SPARQL 1.0 feature: build Triple patterns + BGPs + OPTIONAL
 *
 * Grammar hooks used:
 * - GroupGraphPattern: { ... }  -> builder.enterGroup()/exitGroup()
 * - TriplesBlock: BGP block      -> builder.enterBgp()/exitBgp()
 * - TriplesSameSubject: produce actual triples -> builder.addTriple(s,p,o)
 * - OptionalGraphPattern: OPTIONAL { ... } -> builder.enterOptional()/exitOptional()
 */
public class BgpFeature extends AbstractSparqlFeature {

    private final SparqlAstBuilder builder;

    public BgpFeature(SparqlAstBuilder builder) {
        this.builder = builder;
    }

    // ------------ GROUP { .... } ------------

    @Override
    public void enterGroupGraphPattern(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.GroupGraphPatternContext ctx) {
        builder.enterGroup();
    }

    @Override
    public void exitGroupGraphPattern(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.GroupGraphPatternContext ctx) {
        builder.exitGroup();
    }

    // -------- OPTIONAL { ... } --------
    @Override
    public void enterOptionalGraphPattern(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.OptionalGraphPatternContext ctx) {
        builder.enterOptional();
    }

    @Override
    public void exitOptionalGraphPattern(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.OptionalGraphPatternContext ctx) {
        builder.exitOptional();
    }

    // -------- BGP (TriplesBlock) --------
    @Override
    public void enterTriplesBlock(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.TriplesBlockContext ctx) {
        builder.enterBgp();
    }

    @Override
    public void exitTriplesBlock(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.TriplesBlockContext ctx) {
        builder.exitBgp();
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
            for (TermAst o : objects) { builder.addTriple(s, p, o); }
        }
    }

    // ---- term helpers ----

    private TermAst termFromVerb(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.VerbContext ctx) {
        if (ctx.A() != null) return builder.iri("a");
        return termFromVarOrIriRef(ctx.varOrIRIref());
    }

    private TermAst termFromVarOrTerm(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.VarOrTermContext ctx) {
        if (ctx.var_() != null) return builder.var(ctx.var_().getText());
        return termFromGraphTerm(ctx.graphTerm());
    }

    private TermAst termFromVarOrIriRef(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.VarOrIRIrefContext ctx) {
        String txt = ctx.getText();
        if (txt.startsWith("?") || txt.startsWith("$")) return builder.var(txt);
        return builder.iri(txt);
    }

    private TermAst termFromGraphTerm(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.GraphTermContext ctx) {
        if (ctx.iriRef() != null) { return builder.iri(ctx.iriRef().getText()); }
        if (ctx.rdfLiteral() != null) { return termFromRdfLiteral(ctx.rdfLiteral()); }
        if (ctx.numericLiteral() != null) { return builder.literal(ctx.numericLiteral().getText(), null, null); }
        if (ctx.booleanLiteral() != null) { return builder.literal(ctx.booleanLiteral().getText(), null, null); }
        if (ctx.blankNode() != null) { return builder.iri(ctx.blankNode().getText()); }
        if (ctx.NIL() != null) { return builder.iri("()");} // NIL = () in SPARQL
        return builder.iri(ctx.getText());
    }

    private List<TermAst> termListFromObjectList(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.ObjectListContext ctx) {
        List<TermAst> out = new ArrayList<>();
        for (var obj : ctx.object_()) {
            out.add(termFromObject(obj));
        }
        return out;
    }

    private TermAst termFromObject(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.Object_Context ctx) {
        // object_ : graphNode
        return termFromGraphNode(ctx.graphNode());
    }

    private TermAst termFromGraphNode(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.GraphNodeContext ctx) {
        if (ctx.varOrTerm() != null) return termFromVarOrTerm(ctx.varOrTerm());
        if (ctx.triplesNode() != null) {
            // MVP: pas encore supporté ( [ ... ] ou ( ... ) )
            return builder.iri(ctx.triplesNode().getText());
        }

        return builder.iri(ctx.getText());
    }

    private TermAst termFromRdfLiteral(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.RdfLiteralContext ctx) {
        // rdfLiteral : string_ ( LANGTAG | '^^' iriRef )?

        String lexical = ctx.string_().getText();
        String lang = null;
        String datatype = null;

        if (ctx.LANGTAG() != null) {
            String t = ctx.LANGTAG().getText(); // ex: "@fr"
            lang = t.startsWith("@") ? t.substring(1) : t;
        } else if (ctx.DOUBLE_CARET() != null && ctx.iriRef() != null) {
            datatype = ctx.iriRef().getText(); // ex: xsd:integer ou <iri>
        }
        return builder.literal(lexical, lang, datatype);
    }
}
