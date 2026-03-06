package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;

/**
 * Abstract Syntax Tree (AST) representation of a SPARQL {@code SELECT} query.
 * Holds the projection (SELECT * or SELECT ?v1 ?v2 ...) and the WHERE clause.
 */
public record SelectQueryAst(ProjectionAst projection, GroupGraphPatternAst whereClause) implements QueryAst {

    /** Constructor with default projection SELECT *. */
    public SelectQueryAst(GroupGraphPatternAst whereClause) {
        this(ProjectionAsts.selectAll(), whereClause);
    }

    public SelectQueryAst {
        if (projection == null) {
            projection = ProjectionAsts.selectAll();
        }
        if (whereClause == null) {
            whereClause = new GroupGraphPatternAst(List.of());
        }
    }
}