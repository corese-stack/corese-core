package fr.inria.corese.core.next.query.impl.sparql.ast.path;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

public record PredicatePathAst(TermAst predicate) implements PathAst {
    public PredicatePathAst {
        if (predicate == null) throw new IllegalArgumentException("predicate is null");
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        predicate.accept(visitor);
    }
}
