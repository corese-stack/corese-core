package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;

/**
 * Utility factory for {@link ProjectionAst}.
 * Provides convenient methods to create {@code SELECT *} or explicit projections.
 */
public final class ProjectionAsts {

    private ProjectionAsts() {
    }

    /** SELECT * : project all variables from the WHERE clause. */
    public static ProjectionAst selectAll() {
        return new ProjectionAst(true, List.of());
    }

    /** SELECT ?v1 ?v2 ... : project only the given variables. */
    public static ProjectionAst of(List<VarAst> variables) {
        if (variables == null || variables.isEmpty()) {
            return selectAll();
        }
        return new ProjectionAst(false, List.copyOf(variables));
    }
}
