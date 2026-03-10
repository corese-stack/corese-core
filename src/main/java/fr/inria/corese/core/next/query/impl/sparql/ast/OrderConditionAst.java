package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * One ORDER BY condition.
 */
public record OrderConditionAst(
        OrderDirection orderDirection,
        ExprAst expression
) {
    public OrderConditionAst {
        if (orderDirection == null) throw new IllegalArgumentException("direction is null");
        if (expression == null) throw new IllegalArgumentException("expression is null");
    }
}
