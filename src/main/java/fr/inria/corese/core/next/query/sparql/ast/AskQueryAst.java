package fr.inria.corese.core.next.query.sparql.ast;

import java.util.List;

/**
 * Abstract Syntax Tree (AST) representation of a SPARQL {@code ASK} query.
 * ASK WHERE { pattern } returns a boolean.
 */
public record AskQueryAst(GroupGraphPatternAst whereClause) implements QueryAst {
    public AskQueryAst {
        if (whereClause == null) {
            whereClause = new GroupGraphPatternAst(List.of());
        }
    }
}
