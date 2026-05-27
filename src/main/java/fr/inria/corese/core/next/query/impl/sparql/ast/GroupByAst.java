package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SPARQL {@code GROUP BY} clause metadata.
 *
 * <p>The {@code expressions} list preserves the grouping expressions in source order.
 * When a group condition uses {@code (expr AS ?var)}, the alias is retained in
 * {@code expressionBoundVariables} and {@code expressionTerms} so semantic validation
 * can treat it as an introduced grouped variable.</p>
 */
public record GroupByAst(
        List<TermAst> expressions,
        Set<String> expressionBoundVariables,
        Map<String, TermAst> expressionTerms
) {
    public GroupByAst(List<TermAst> expressions) {
        this(expressions, Set.of(), Map.of());
    }

    public GroupByAst {
        expressions = expressions != null ? List.copyOf(expressions) : List.of();
        expressionBoundVariables = expressionBoundVariables != null ? Set.copyOf(expressionBoundVariables) : Set.of();
        expressionTerms = expressionTerms != null ? Map.copyOf(expressionTerms) : Map.of();
    }

    public boolean isEmpty() {
        return expressions.isEmpty();
    }
}
