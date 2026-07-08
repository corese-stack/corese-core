package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

import java.util.List;

/**
 * Minimal AST representation of {@code INSERT DATA} for concrete triples.
 */
public record InsertDataRequestAst(List<TriplePatternAst> triples) implements UpdateRequestUnitAst {

    public InsertDataRequestAst {
        triples = triples != null ? List.copyOf(triples) : List.of();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        triples.forEach(triple -> triple.accept(visitor));
    }
}
