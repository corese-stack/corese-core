package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PredicatePathAst;

import java.util.ArrayList;
import java.util.List;

/**
 * Expands SPARQL property list paths, blank node property lists, and RDF collections into triple patterns.
 */
final class TriplePathExpander {

    private final SparqlAstBuilder builder;
    private final SparqlTermBuilder terms;
    private final SparqlPathBuilder paths;

    TriplePathExpander(final SparqlAstBuilder builder, final SparqlTermBuilder terms, final SparqlPathBuilder paths) {
        this.builder = builder;
        this.terms = terms;
        this.paths = paths;
    }

    /**
     * Expands a {@code propertyListPathNotEmpty} into triple patterns for the given subject.
     *
     * @param subject      subject term for the expanded triple patterns
     * @param propertyList parse context containing verb paths, simple verbs, and object list paths
     */
    void addTriplesFromPropertyListPath(
            final TermAst subject,
            final SparqlParser.PropertyListPathNotEmptyContext propertyList) {
        final var verbPaths = propertyList.verbPath();
        final var verbSimples = propertyList.verbSimple();
        final var objectLists = propertyList.objectListPath();

        int verbPathIdx = 0;
        int verbSimpleIdx = 0;

        for (final SparqlParser.ObjectListPathContext objectList : objectLists) {
            final boolean useVerbPath = verbPathIdx < verbPaths.size()
                    && (verbSimpleIdx >= verbSimples.size()
                    || verbPaths.get(verbPathIdx).getStart().getTokenIndex()
                    < verbSimples.get(verbSimpleIdx).getStart().getTokenIndex());

            final PathAst predicate = useVerbPath
                    ? paths.pathFromVerbPath(verbPaths.get(verbPathIdx++))
                    : new PredicatePathAst(terms.termFromVerbSimple(verbSimples.get(verbSimpleIdx++)));

            for (final TermAst object : termListFromObjectListPath(objectList)) {
                builder.addTriple(subject, predicate, object);
            }
        }
    }

    /**
     * Builds triple patterns for a {@code triplesNodePath} subject and returns its head term.
     *
     * @param ctx parse context for triples node path
     * @return head term of the expanded structure
     */
    TermAst subjectFromTriplesNodePath(final SparqlParser.TriplesNodePathContext ctx) {
        if (ctx.blankNodePropertyListPath() != null) {
            return subjectFromBlankNodePropertyListPath(ctx.blankNodePropertyListPath());
        }
        if (ctx.collectionPath() != null) {
            return subjectFromCollectionPath(ctx.collectionPath());
        }
        throw new QueryEvaluationException("Unexpected triples node path: " + ctx.getText());
    }

    private TermAst subjectFromBlankNodePropertyListPath(
            final SparqlParser.BlankNodePropertyListPathContext ctx) {
        final TermAst blankNode = builder.newAnonymousBlankNode();
        if (ctx.propertyListPathNotEmpty() != null) {
            addTriplesFromPropertyListPath(blankNode, ctx.propertyListPathNotEmpty());
        }
        return blankNode;
    }

    private TermAst subjectFromCollectionPath(final SparqlParser.CollectionPathContext ctx) {
        final List<SparqlParser.GraphNodePathContext> nodes = ctx.graphNodePath();
        if (nodes.isEmpty()) {
            throw new QueryEvaluationException("Empty RDF collection in triple pattern");
        }

        final TermAst head = builder.newAnonymousBlankNode();
        TermAst current = head;
        for (int i = 0; i < nodes.size(); i++) {
            builder.addTriple(current, rdfTerm(RDF.first), termFromGraphNodePath(nodes.get(i)));
            if (i == nodes.size() - 1) {
                builder.addTriple(current, rdfTerm(RDF.rest), rdfTerm(RDF.nil));
            } else {
                final TermAst next = builder.newAnonymousBlankNode();
                builder.addTriple(current, rdfTerm(RDF.rest), next);
                current = next;
            }
        }
        return head;
    }

    /**
     * Graph node inside a property-path triple (variable/term, blank node property list, or collection).
     *
     * @param ctx parse context for graph node path
     * @return lowered term
     */
    TermAst termFromGraphNodePath(final SparqlParser.GraphNodePathContext ctx) {
        if (ctx.varOrTerm() != null) {
            return terms.termFromVarOrTerm(ctx.varOrTerm());
        }
        if (ctx.triplesNodePath() != null) {
            return subjectFromTriplesNodePath(ctx.triplesNodePath());
        }
        throw new QueryEvaluationException("Unexpected graph node path: " + ctx.getText());
    }

    private static TermAst rdfTerm(final RDF term) {
        return new IriAst("<" + term.getIRI().stringValue() + ">");
    }

    /**
     * List of objects inside a property path triple.
     *
     * @param ctx parse context for object list path
     * @return lowered terms
     */
    @SuppressWarnings("java:S9391") // Preserve ordered accumulation without allocating a stream pipeline.
    List<TermAst> termListFromObjectListPath(final SparqlParser.ObjectListPathContext ctx) {
        final List<TermAst> out = new ArrayList<>();
        for (final var objPath : ctx.objectPath()) {
            out.add(termFromGraphNodePath(objPath.graphNodePath()));
        }
        return out;
    }
}
