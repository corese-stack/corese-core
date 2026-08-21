package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.VisitableAst;

/**
 * One ORDER BY condition.
 */
public record OrderConditionAst(
        ASTConstants.OrderDirection orderDirection,
        TermAst expression
) implements VisitableAst {
    public OrderConditionAst {
        if (orderDirection == null) throw new IllegalArgumentException("direction is null");
        if (expression == null) throw new IllegalArgumentException("expression is null");
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.expression.accept(visitor);
    }
}
