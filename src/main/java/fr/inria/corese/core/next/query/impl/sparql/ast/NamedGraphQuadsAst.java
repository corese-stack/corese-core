package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.VisitableAst;

import java.util.List;

/**
 * A {@code GRAPH varOrIri { triplesTemplate? }} block inside a {@code quads} body.
 *
 * @param graph   the named graph IRI or variable
 * @param triples the triple patterns inside the named graph block
 */
public record NamedGraphQuadsAst(TermAst graph, List<TriplePatternAst> triples) implements VisitableAst {
    public NamedGraphQuadsAst {
        if (graph == null) throw new IllegalArgumentException("graph must be non-null");
        triples = triples != null ? List.copyOf(triples) : List.of();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        graph.accept(visitor);
        triples.forEach(t -> t.accept(visitor));
    }
}
