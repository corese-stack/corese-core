package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlQueryAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import org.antlr.v4.runtime.RuleContext;

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
        if (ctx.constructTemplate() == null) {
            queryBuilder().enterConstructTemplate();
            queryBuilder().enterGroup();
            queryBuilder().enterBgp();
        }
    }

    @Override
    public void exitConstructQuery(SparqlParser.ConstructQueryContext ctx) {
        if (ctx.constructTemplate() == null) {
            queryBuilder().exitBgp();
            queryBuilder().exitGroup();
            queryBuilder().exitConstructTemplate();
        }
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
     * Handles {@code triplesSameSubject} nodes inside the CONSTRUCT template
     * or inside the short-form {@code CONSTRUCT WHERE { triplesTemplate }}.
     */
    @Override
    public void exitTriplesSameSubject(SparqlParser.TriplesSameSubjectContext ctx) {
        boolean inConstructTriples = ctx.getParent() instanceof SparqlParser.ConstructTriplesContext;
        boolean inConstructWhere = isConstructWhere(ctx);
        if (!inConstructTriples && !inConstructWhere) {
            return;
        }
        if (ctx.varOrTerm() != null && ctx.propertyListNotEmpty() != null) {
            TermAst subject = queryBuilder().termFromVarOrTerm(ctx.varOrTerm());
            addConstructProperties(subject, ctx.propertyListNotEmpty(), inConstructWhere);
        } else if (ctx.triplesNode() != null) {
            TermAst subject = subjectFromTriplesNode(ctx.triplesNode(), inConstructWhere);
            if (ctx.propertyList() != null && ctx.propertyList().propertyListNotEmpty() != null) {
                addConstructProperties(subject, ctx.propertyList().propertyListNotEmpty(), inConstructWhere);
            }
        }
    }

    /**
     * Expands a {@code triplesNode} into the CONSTRUCT template (and optionally the WHERE BGP),
     * returning the head term (an anonymous blank node for blank node property lists).
     */
    private TermAst subjectFromTriplesNode(SparqlParser.TriplesNodeContext ctx, boolean inConstructWhere) {
        if (ctx.blankNodePropertyList() != null) {
            TermAst blankNode = queryBuilder().newAnonymousBlankNode();
            var inner = ctx.blankNodePropertyList().propertyListNotEmpty();
            if (inner != null) {
                addConstructProperties(blankNode, inner, inConstructWhere);
            }
            return blankNode;
        }
        if (ctx.collection() != null) {
            return subjectFromCollection(ctx.collection(), inConstructWhere);
        }
        return queryBuilder().iri(ctx.getText());
    }

    /**
     * Expands an RDF collection {@code (e1 e2 ...)} into rdf:first/rdf:rest chains
     * in the CONSTRUCT template and returns the head blank node.
     */
    private TermAst subjectFromCollection(SparqlParser.CollectionContext ctx, boolean inConstructWhere) {
        var nodes = ctx.graphNode();
        if (nodes.isEmpty()) {
            return queryBuilder().iri("<http://www.w3.org/1999/02/22-rdf-syntax-ns#nil>");
        }
        TermAst first = queryBuilder().iri("<http://www.w3.org/1999/02/22-rdf-syntax-ns#first>");
        TermAst rest = queryBuilder().iri("<http://www.w3.org/1999/02/22-rdf-syntax-ns#rest>");
        TermAst nil = queryBuilder().iri("<http://www.w3.org/1999/02/22-rdf-syntax-ns#nil>");
        TermAst head = queryBuilder().newAnonymousBlankNode();
        TermAst current = head;
        for (int i = 0; i < nodes.size(); i++) {
            TermAst element = queryBuilder().termFromGraphNode(nodes.get(i));
            queryBuilder().addConstructTriple(current, first, element);
            if (inConstructWhere) queryBuilder().addTriple(current, first, element);
            if (i == nodes.size() - 1) {
                queryBuilder().addConstructTriple(current, rest, nil);
                if (inConstructWhere) queryBuilder().addTriple(current, rest, nil);
            } else {
                TermAst next = queryBuilder().newAnonymousBlankNode();
                queryBuilder().addConstructTriple(current, rest, next);
                if (inConstructWhere) queryBuilder().addTriple(current, rest, next);
                current = next;
            }
        }
        return head;
    }

    private void addConstructProperties(
            TermAst subject,
            SparqlParser.PropertyListNotEmptyContext propertyList,
            boolean inConstructWhere) {
        for (int verbIndex = 0; verbIndex < propertyList.verb().size(); verbIndex++) {
            TermAst predicate = queryBuilder().termFromVerb(propertyList.verb(verbIndex));
            List<TermAst> objects = queryBuilder().termListFromObjectList(propertyList.objectList(verbIndex));
            for (TermAst object : objects) {
                queryBuilder().addConstructTriple(subject, predicate, object);
                if (inConstructWhere) {
                    queryBuilder().addTriple(subject, predicate, object);
                }
            }
        }
    }

    private boolean isConstructWhere(SparqlParser.TriplesSameSubjectContext ctx) {
        RuleContext parent = ctx.getParent();
        while (parent instanceof SparqlParser.TriplesTemplateContext) {
            parent = parent.getParent();
        }
        return parent instanceof SparqlParser.ConstructQueryContext;
    }
}
