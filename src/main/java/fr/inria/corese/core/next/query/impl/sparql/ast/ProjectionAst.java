package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;

/**
 * SPARQL SELECT projection: either {@code SELECT *} (all variables from the pattern)
 * or an explicit list of variables {@code SELECT ?s ?p ?o}.
 * <p>
 * Use {@link ProjectionAsts#selectAll()} and {@link ProjectionAsts#of(List)} to create instances.
 */


public record ProjectionAst(boolean selectAll, List<VarAst> variables) {
    /**
     * Creates a SPARQL SELECT Projection
     * @param selectAll {@code true} if the projection corresponds to {@code SELECT *},
     *                  meaning all variables from the WHERE clause are projected;
     *                  {@code false} if the projection explicitly lists variables.
     * @param variables the variables explicitly projected by the SELECT clause.
     *                  When {@code selectAll} is {@code true}, this list must be empty.
     * @throws IllegalArgumentException if {@code selectAll} is {@code true} and
      *                                  {@code variables} is non-empty
     *                                  if {@code selectAll} is {@code false} and {@code variables} is empty
     */
    public ProjectionAst {
        variables = variables != null ? List.copyOf(variables) : List.of();
        if (selectAll && !variables.isEmpty()) {
            throw new IllegalArgumentException("selectAll is true but variables is non-empty");
        }
        if (!selectAll && variables.isEmpty()) {
            throw new IllegalArgumentException("Explicit projection is empty (use selectAll=true for SELECT *)");
        }
    }
}
