package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;

/**
 * SPARQL solution modifiers applied after pattern evaluation.
 *
 *  <p>
 *      <ul>
 *          <li>DISTINCT / REDUCED<li/>
 *          <li>ORDER BY<li/>
 *          <li>LIMIT<li/>
 *          <li>OFFSET<li/>
 *      <ul/>
 *  <p/>
 *
 */
public record SolutionModifierAst(
        boolean distinct,
        boolean reduced,
        List<OrderConditionAst> orderBy,
        Long limit,
        Long offset
) {
    public SolutionModifierAst {
        orderBy = orderBy != null ? List.copyOf(orderBy) : List.of();

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

    public static SolutionModifierAst empty() {
        return new SolutionModifierAst(false, false, List.of(), null, null);
    }

    public boolean hasOrderBy() {
        return !orderBy.isEmpty();
    }

    public boolean hasLimit() {
        return limit != null;
    }

    public boolean hasOffset() {
        return offset != null;
    }
}
