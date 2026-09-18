package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.NamedGraphQuadsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QuadsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;

import java.util.ArrayList;
import java.util.List;

/** Quad-template lowering, including nested blank-node property lists and RDF lists. */
final class SparqlQuadTemplateBuilder {
    private final SparqlAstBuilder terms;

    SparqlQuadTemplateBuilder(SparqlAstBuilder terms) {
        this.terms = terms;
    }

    QuadsAst quads(SparqlParser.QuadsContext context) {
        List<TriplePatternAst> defaults = new ArrayList<>();
        List<NamedGraphQuadsAst> named = new ArrayList<>();
        for (var template : context.triplesTemplate()) {
            defaults.addAll(triples(template));
        }
        for (var graph : context.quadsNotTriples()) {
            named.add(new NamedGraphQuadsAst(terms.termFromVarOrIriRef(graph.varOrIri()),
                    triples(graph.triplesTemplate())));
        }
        return new QuadsAst(defaults, named);
    }

    private List<TriplePatternAst> triples(SparqlParser.TriplesTemplateContext context) {
        List<TriplePatternAst> result = new ArrayList<>();
        for (var current = context; current != null; current = current.triplesTemplate()) {
            var triple = current.triplesSameSubject();
            if (triple.varOrTerm() != null) {
                properties(terms.termFromVarOrTerm(triple.varOrTerm()), triple.propertyListNotEmpty(), result);
            } else {
                TermAst subject = nested(triple.triplesNode(), result);
                properties(subject, triple.propertyList().propertyListNotEmpty(), result);
            }
        }
        return result;
    }

    private void properties(TermAst subject, SparqlParser.PropertyListNotEmptyContext properties,
            List<TriplePatternAst> result) {
        if (properties == null) {
            return;
        }
        for (int index = 0; index < properties.verb().size(); index++) {
            TermAst predicate = terms.termFromVerb(properties.verb(index));
            for (var object : properties.objectList(index).object_()) {
                result.add(new TriplePatternAst(subject, predicate, node(object.graphNode(), result)));
            }
        }
    }

    private TermAst node(SparqlParser.GraphNodeContext context, List<TriplePatternAst> result) {
        return context.varOrTerm() == null ? nested(context.triplesNode(), result)
                : terms.termFromVarOrTerm(context.varOrTerm());
    }

    private TermAst nested(SparqlParser.TriplesNodeContext context, List<TriplePatternAst> result) {
        TermAst head = terms.newAnonymousBlankNode();
        if (context.blankNodePropertyList() != null) {
            properties(head, context.blankNodePropertyList().propertyListNotEmpty(), result);
        } else {
            collection(head, context.collection().graphNode(), result);
        }
        return head;
    }

    private void collection(TermAst head, List<SparqlParser.GraphNodeContext> nodes, List<TriplePatternAst> result) {
        TermAst current = head;
        for (int index = 0; index < nodes.size(); index++) {
            result.add(new TriplePatternAst(current, rdf(RDF.first), node(nodes.get(index), result)));
            TermAst next = index == nodes.size() - 1 ? rdf(RDF.nil) : terms.newAnonymousBlankNode();
            result.add(new TriplePatternAst(current, rdf(RDF.rest), next));
            current = next;
        }
    }

    private static IriAst rdf(RDF term) {
        return new IriAst("<" + term.getIRI().stringValue() + ">");
    }
}
