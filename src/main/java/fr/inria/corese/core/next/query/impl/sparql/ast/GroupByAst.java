package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.VisitableAst;

public record GroupByAst(List<TermAst> expressions) implements VisitableAst {
    public GroupByAst {
        expressions = expressions != null ? List.copyOf(expressions) : List.of();
    }

    public boolean isEmpty() {
        return expressions.isEmpty();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.expressions.forEach(termAst -> termAst.accept(visitor));
    }
}
