package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * SPARQL CONSTRUCT query feature: sets query type, collects the CONSTRUCT template
 * (triples to instantiate from WHERE bindings) and delegates the WHERE clause to {@link BgpFeature}.
 *
 * <p>Grammar: {@code CONSTRUCT constructTemplate ... whereClause ...}
 * The template uses the same {@code triplesSameSubject} structure as the WHERE clause.
 * Triples in the template are emitted via {@link AbstractTripleEmitterFeature} when the parent
 * is {@code ConstructTriplesContext} → builder.addConstructTriple(subject, predicate, object).
 */
public class ConstructQueryFeature extends AbstractTripleEmitterFeature {

    public ConstructQueryFeature(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void enterConstructQuery(SparqlParser.ConstructQueryContext ctx) {
        builder.enterConstructQuery();
    }

    @Override
    public void exitConstructQuery(SparqlParser.ConstructQueryContext ctx) {
        builder.exitConstructQuery();
    }

    @Override
    public void enterConstructTemplate(SparqlParser.ConstructTemplateContext ctx) {
        builder.enterConstructTemplate();
    }

    @Override
    public void exitConstructTemplate(SparqlParser.ConstructTemplateContext ctx) {
        builder.exitConstructTemplate();
    }

    @Override
    protected boolean shouldHandleTriplesSameSubject(SparqlParser.TriplesSameSubjectContext ctx) {
        return ctx.getParent() instanceof SparqlParser.ConstructTriplesContext;
    }

    @Override
    protected void emitTriple(TermAst subject, TermAst predicate, TermAst object) {
        builder.addConstructTriple(subject, predicate, object);
    }
}
