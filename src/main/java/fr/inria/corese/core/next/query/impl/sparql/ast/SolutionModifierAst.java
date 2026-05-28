package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.VisitableAst;

import java.util.List;

/**
 * AST for SPARQL {@code solutionModifier}, plus DISTINCT and REDUCED as represented in this project:
 * {@code GROUP BY}, {@code DISTINCT} / {@code REDUCED} (syntactically on {@code SELECT} but stored here),
 * {@code ORDER BY}, {@code LIMIT}, {@code OFFSET}.
 *
 * <ul>
 *   <li>GROUP BY</li>
 *   <li>DISTINCT / REDUCED</li>
 *   <li>HAVING</li>
 *   <li>ORDER BY</li>
 *   <li>LIMIT / OFFSET</li>
 * </ul>
 * <p>In SPARQL 1.1 grammar: {@code groupClause? havingClause? orderClause? limitOffsetClauses?}.
 */
public record SolutionModifierAst(
        GroupByAst groupBy,
        boolean distinct,
        boolean reduced,
        List<OrderConditionAst> orderBy,
        HavingAst having,
        Long limit,
        Long offset
) implements VisitableAst {
    public SolutionModifierAst {
        groupBy = groupBy != null ? groupBy : new GroupByAst(List.of());
        orderBy = orderBy != null ? List.copyOf(orderBy) : List.of();
        having = having != null ? having : HavingAst.empty();
        if (distinct && reduced) {
            throw new IllegalArgumentException("DISTINCT and REDUCED are mutually exclusive");
        }
        if (limit != null && limit < 0) {
            throw new IllegalArgumentException("LIMIT must be >= 0");
        }
        if (offset != null && offset < 0) {
            throw new IllegalArgumentException("OFFSET must be >= 0");
        }
    }

    /**
     * Default empty modifiers: no GROUP BY, no DISTINCT/REDUCED, no ORDER BY, no LIMIT or OFFSET.
     */
    public static SolutionModifierAst empty() {
        return new SolutionModifierAst(new GroupByAst(List.of()), false, false, List.of(), HavingAst.empty(), null, null);
    }

    /**
     * Builds modifiers without an explicit {@code GROUP BY}, matching the shape from before {@link GroupByAst} was added.
     */
    public static SolutionModifierAst withoutGroupBy(
            boolean distinct,
            boolean reduced,
            List<OrderConditionAst> orderBy,
            Long limit,
            Long offset) {
        return new SolutionModifierAst(new GroupByAst(List.of()), distinct, reduced, orderBy, HavingAst.empty(), limit, offset);
    }

    public boolean hasGroupBy() {
        return !groupBy.isEmpty();
    }

    public boolean hasOrderBy() {
        return !orderBy.isEmpty();
    }

    public boolean hasHaving() { return !having.isEmpty(); }

    public boolean hasLimit() {
        return limit != null;
    }

    public boolean hasOffset() {
        return offset != null;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        groupBy.accept(visitor);
        this.orderBy.forEach(orderConditionAst -> {
            orderConditionAst.accept(visitor);
        });
        this.having.accept(visitor);
    }
}
