package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.VisitableAst;

import java.util.List;

/**
 * Represents the body of a {@code quadData} or {@code quadPattern} block as defined in
 * <a href="https://www.w3.org/TR/sparql11-update/">SPARQL 1.1 Update</a>.
 *
 * @param defaultTriples   triple patterns targeting the default graph
 * @param namedGraphBlocks named-graph triple blocks
 */
public record QuadsAst(List<TriplePatternAst> defaultTriples, List<NamedGraphQuadsAst> namedGraphBlocks)
        implements VisitableAst {
    public QuadsAst {
        defaultTriples = defaultTriples != null ? List.copyOf(defaultTriples) : List.of();
        namedGraphBlocks = namedGraphBlocks != null ? List.copyOf(namedGraphBlocks) : List.of();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        defaultTriples.forEach(t -> t.accept(visitor));
        namedGraphBlocks.forEach(b -> b.accept(visitor));
    }
}
