package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

import java.util.List;

/**
 * Basic Graph Pattern: a list of triple patterns (TriplesBlock).
 */
public record BgpAst(List<TriplePatternAst> triples) implements PatternAst {
    public BgpAst {
        triples = triples != null ? List.copyOf(triples) : List.of();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        triples.forEach(triplePatternAst -> triplePatternAst.accept(visitor));
    }
}
