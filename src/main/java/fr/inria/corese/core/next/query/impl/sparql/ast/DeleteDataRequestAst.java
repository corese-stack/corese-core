package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

import java.util.List;

/**
 * Minimal AST representation of {@code DELETE DATA} for concrete triples.
 */
public record DeleteDataRequestAst(List<TriplePatternAst> triples) implements UpdateRequestUnitAst {

    public DeleteDataRequestAst {
        triples = triples != null ? List.copyOf(triples) : List.of();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        triples.forEach(triple -> triple.accept(visitor));
    }
}
