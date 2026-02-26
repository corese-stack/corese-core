package fr.inria.corese.core.next.query.api.sparql.ast;

import java.util.List;

/**
 * Abstract Syntax Tree (AST) representation of a SPARQL {@code SELECT} query.
 */
public record SelectQueryAst(GroupGraphPatternAst whereClause) implements QueryAst {
    public SelectQueryAst {
        if (whereClause == null) {
            whereClause = new GroupGraphPatternAst(List.of());
        }
    }
}
