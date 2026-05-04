package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;
import java.util.Set;

/**
 * SPARQL SELECT projection: either {@code SELECT *} (all variables from the pattern)
 * or an explicit list of variables {@code SELECT ?s ?p ?o}.
 * <p>
 * Use {@link ProjectionAsts#selectAll()} and {@link ProjectionAsts#of(List)} to create instances.
 */
public record ProjectionAst(boolean selectAll, List<VarAst> variables, Set<String> expressionBoundVariables) {
    public ProjectionAst {
        variables = variables != null ? List.copyOf(variables) : List.of();
        expressionBoundVariables = expressionBoundVariables != null ? Set.copyOf(expressionBoundVariables) : Set.of();
        if (selectAll && !variables.isEmpty()) {
            throw new IllegalArgumentException("selectAll is true but variables is non-empty");
        }
        if (!selectAll && variables.isEmpty()) {
            throw new IllegalArgumentException("Explicit projection is empty (use selectAll=true for SELECT *)");
        }
    }
}
