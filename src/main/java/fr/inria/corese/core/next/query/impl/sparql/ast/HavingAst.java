package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

import java.util.List;

/**
 * SPARQL {@code HAVING} clause: boolean constraints evaluated after grouping.
 */
public record HavingAst(List<TermAst> conditions) implements VisitableAst {
    public HavingAst {
        conditions = conditions != null ? List.copyOf(conditions) : List.of();
    }

    public boolean isEmpty() {
        return conditions.isEmpty();
    }

    public static HavingAst empty() {
        return new HavingAst(List.of());
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.conditions.forEach(conditionAst -> {
            conditionAst.accept(visitor);
        });
    }
}
