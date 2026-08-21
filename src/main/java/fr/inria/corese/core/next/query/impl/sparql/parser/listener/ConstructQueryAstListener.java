package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlQueryAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * SPARQL CONSTRUCT query feature: sets query type, collects the CONSTRUCT template
 * (triples to instantiate from WHERE bindings) and delegates the WHERE clause to {@link BgpAstListener}.
 *
 * <p>Grammar: {@code CONSTRUCT constructTemplate ... whereClause ...}
 * The template uses the same {@code triplesSameSubject} structure as the WHERE clause.
 * Triples in the template are emitted when the parent is {@code ConstructTriplesContext},
 * using {@link SparqlAstBuilder} term helpers → {@link SparqlQueryAstBuilder#addConstructTriple}.
 */
public class ConstructQueryAstListener extends AbstractSparqlAstListener implements QueryAstListener {

    public ConstructQueryAstListener(SparqlQueryAstBuilder builder) {
        super(builder);
    }

    public SparqlQueryAstBuilder queryBuilder() {
        return (SparqlQueryAstBuilder) builder();
    }

    @Override
    public void enterConstructQuery(SparqlParser.ConstructQueryContext ctx) {
        queryBuilder().enterConstructQuery();
    }

    @Override
    public void exitConstructQuery(SparqlParser.ConstructQueryContext ctx) {
        queryBuilder().exitConstructQuery();
    }

    @Override
    public void enterConstructTemplate(SparqlParser.ConstructTemplateContext ctx) {
        queryBuilder().enterConstructTemplate();
    }

    @Override
    public void exitConstructTemplate(SparqlParser.ConstructTemplateContext ctx) {
        queryBuilder().exitConstructTemplate();
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
        TermAst subject = queryBuilder().termFromVarOrTerm(ctx.varOrTerm());
        var propertyList = ctx.propertyListNotEmpty();
        for (int verbIndex = 0; verbIndex < propertyList.verb().size(); verbIndex++) {
            TermAst predicate = queryBuilder().termFromVerb(propertyList.verb(verbIndex));
            List<TermAst> objects = queryBuilder().termListFromObjectList(propertyList.objectList(verbIndex));
            for (TermAst object : objects) {
                queryBuilder().addConstructTriple(subject, predicate, object);
            }
        }
    }
}
