package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;

/**
 * SPARQL 1.0 feature: build triple patterns, BGPs and OPTIONAL for the WHERE clause.
 *
 * <p>Grammar hooks:
 * <ul>
 *   <li>GroupGraphPattern: {@code { ... } } → builder.enterGroup() / exitGroup()</li>
 *   <li>TriplesBlock: BGP boundary → builder.enterBgp() / exitBgp()</li>
 *   <li>TriplesSameSubject (when not inside CONSTRUCT template): emit via
 *       {@link AbstractTripleEmitterFeature} → builder.addTriple(subject, predicate, object)</li>
 *   <li>OptionalGraphPattern: {@code OPTIONAL { ... } } → builder.enterOptional() / exitOptional()</li>
 * </ul>
 */
public class BgpFeature extends AbstractTripleEmitterFeature {

    public BgpFeature(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void enterGroupGraphPattern(SparqlParser.GroupGraphPatternContext ctx) {
        builder.enterGroup();
    }

    @Override
    public void exitGroupGraphPattern(SparqlParser.GroupGraphPatternContext ctx) {
        builder.exitGroup();
    }

    @Override
    public void enterTriplesBlock(SparqlParser.TriplesBlockContext ctx) {
        builder.enterBgp();
    }

    @Override
    public void exitTriplesBlock(SparqlParser.TriplesBlockContext ctx) {
        builder.exitBgp();
    }

    @Override
    public void enterOptionalGraphPattern(SparqlParser.OptionalGraphPatternContext ctx) {
        builder.enterOptional();
    }

    @Override
    public void enterTriplesBlock(SparqlParser.TriplesBlockContext ctx) {
        builder.enterBgp();
    public void exitOptionalGraphPattern(SparqlParser.OptionalGraphPatternContext ctx) {
        builder.exitOptional();
    }

    @Override
    public void enterTriplesBlock(SparqlParser.TriplesBlockContext ctx) {
        builder.enterBgp();
    protected boolean shouldHandleTriplesSameSubject(SparqlParser.TriplesSameSubjectContext ctx) {
        return !(ctx.getParent() instanceof SparqlParser.ConstructTriplesContext);
    }

    @Override
    public void exitTriplesBlock(SparqlParser.TriplesBlockContext ctx) {
        builder.exitBgp();
    }

    @Override
    public void enterOptionalGraphPattern(SparqlParser.OptionalGraphPatternContext ctx) {
        builder.enterOptional();
    }

    @Override
    public void exitOptionalGraphPattern(SparqlParser.OptionalGraphPatternContext ctx) {
        builder.exitOptional();
    }

    @Override
    protected boolean shouldHandleTriplesSameSubject(SparqlParser.TriplesSameSubjectContext ctx) {
        return !(ctx.getParent() instanceof SparqlParser.ConstructTriplesContext);
    public void exitTriplesSameSubject(fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.TriplesSameSubjectContext ctx) {
        // Only handle triples inside a TriplesBlock (WHERE). Ignore triples inside CONSTRUCT template.
        if (ctx.getParent() instanceof fr.inria.corese.core.next.impl.parser.antlr.SparqlParser.ConstructTriplesContext) {
            return;
        }
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

    @Override
    protected void emitTriple(TermAst subject, TermAst predicate, TermAst object) {
        builder.addTriple(subject, predicate, object);
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

    @Override
    protected void emitTriple(TermAst subject, TermAst predicate, TermAst object) {
        builder.addTriple(subject, predicate, object);
    }
}
