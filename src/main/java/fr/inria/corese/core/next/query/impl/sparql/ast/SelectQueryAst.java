package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;

/**
 * Abstract Syntax Tree (AST) representation of a SPARQL {@code SELECT} query.
 * Holds the projection (SELECT * or SELECT ?v1 ?v2 ...) and the WHERE clause.
 */
public record SelectQueryAst(ProjectionAst projection, GroupGraphPatternAst whereClause, SolutionModifierAst solutionModifier) implements QueryAst {

    /** Constructor with default projection SELECT *. */
    public SelectQueryAst(GroupGraphPatternAst whereClause) {
        this(ProjectionAsts.selectAll(), whereClause);
    }

    /** Constructor with default solution modifier (no DISTINCT/REDUCED/ORDER BY/LIMIT/OFFSET). */
    public SelectQueryAst(ProjectionAst projection, GroupGraphPatternAst whereClause) {
        this(projection, whereClause, null);
    }

    public SelectQueryAst {
        if (projection == null) {
            projection = ProjectionAsts.selectAll();
        }
        if (whereClause == null) {
            whereClause = new GroupGraphPatternAst(List.of());
        }
        if (solutionModifier == null) {
            solutionModifier = SolutionModifierAst.empty();
        }
    }
}