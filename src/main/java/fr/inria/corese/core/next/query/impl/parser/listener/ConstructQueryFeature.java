package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * SPARQL CONSTRUCT query feature: sets query type, collects the CONSTRUCT template
 * (triples to instantiate from WHERE bindings) and delegates the WHERE clause to {@link BgpFeature}.
 *
 * <p>Grammar: {@code CONSTRUCT constructTemplate ... whereClause ...}
 * The template uses the same {@code triplesSameSubject} structure as the WHERE clause.
 * Triples in the template are emitted when the parent is {@code ConstructTriplesContext},
 * using {@link SparqlAstBuilder} term helpers → {@link SparqlAstBuilder#addConstructTriple}.
 */
public class ConstructQueryFeature extends AbstractSparqlFeature {

    public ConstructQueryFeature(SparqlAstBuilder builder) {
        super(builder);
    }

    @Override
    public void enterConstructQuery(SparqlParser.ConstructQueryContext ctx) {
        builder().enterConstructQuery();
    }

    @Override
    public void exitConstructQuery(SparqlParser.ConstructQueryContext ctx) {
        builder().exitConstructQuery();
    }

    @Override
    public void enterConstructTemplate(SparqlParser.ConstructTemplateContext ctx) {
        builder().enterConstructTemplate();
    }

    @Override
    public void exitConstructTemplate(SparqlParser.ConstructTemplateContext ctx) {
        builder().exitConstructTemplate();
    }

    /**
     * Only handles {@code triplesSameSubject} nodes inside the CONSTRUCT template (not the WHERE BGP).
     */
    @Override
    public void exitTriplesSameSubject(SparqlParser.TriplesSameSubjectContext ctx) {
        if (!(ctx.getParent() instanceof SparqlParser.ConstructTriplesContext)) {
            return;
        }
        if (ctx.varOrTerm() == null || ctx.propertyListNotEmpty() == null) {
            return;
        }
        SparqlAstBuilder b = builder();
        TermAst subject = b.termFromVarOrTerm(ctx.varOrTerm());
        var propertyList = ctx.propertyListNotEmpty();
        for (int verbIndex = 0; verbIndex < propertyList.verb().size(); verbIndex++) {
            TermAst predicate = b.termFromVerb(propertyList.verb(verbIndex));
            List<TermAst> objects = b.termListFromObjectList(propertyList.objectList(verbIndex));
            for (TermAst object : objects) {
                b.addConstructTriple(subject, predicate, object);
            }
        }
    }
}
