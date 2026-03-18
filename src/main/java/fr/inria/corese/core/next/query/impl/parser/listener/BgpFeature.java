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
    public void exitOptionalGraphPattern(SparqlParser.OptionalGraphPatternContext ctx) {
        builder.exitOptional();
    }

    @Override
    protected boolean shouldHandleTriplesSameSubject(SparqlParser.TriplesSameSubjectContext ctx) {
        return !(ctx.getParent() instanceof SparqlParser.ConstructTriplesContext);
    }

    @Override
    protected void emitTriple(TermAst subject, TermAst predicate, TermAst object) {
        builder.addTriple(subject, predicate, object);
    }
}
