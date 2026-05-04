package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;
import java.util.Set;

/**
 * Utility factory for {@link ProjectionAst}.
 * Provides convenient methods to create {@code SELECT *} or explicit projections.
 */
public final class ProjectionAsts {

    private ProjectionAsts() {
    }

    /** SELECT * : project all variables from the WHERE clause. */
    public static ProjectionAst selectAll() {
        return new ProjectionAst(true, List.of(), Set.of());
    }

    /** SELECT ?v1 ?v2 ... : project only the given variables. */
    public static ProjectionAst of(List<VarAst> variables) {
        if (variables == null || variables.isEmpty()) {
            return selectAll();
        }
        return new ProjectionAst(false, List.copyOf(variables), Set.of());
    }

    /**
     * SELECT ?v1 (expr AS ?v2) ... : project plain variables and expression-bound variables.
     * Expression-bound variables are those introduced by {@code (expr AS ?var)} in the SELECT clause;
     * they are included in {@code variables} but are not required to be visible in the WHERE clause.
     */
    public static ProjectionAst of(List<VarAst> variables, Set<String> expressionBoundVariables) {
        if (variables == null || variables.isEmpty()) {
            return selectAll();
        }
        return new ProjectionAst(false, List.copyOf(variables), expressionBoundVariables != null ? expressionBoundVariables : Set.of());
    }
}
