package fr.inria.corese.core.next.query.sparql.ast;

import java.util.List;

/**
 * Abstract Syntax Tree (AST) representation of a SPARQL {@code CONSTRUCT} query.
 * CONSTRUCT { template } WHERE { pattern }.
 */
public record ConstructQueryAst(GroupGraphPatternAst constructTemplate, GroupGraphPatternAst whereClause) implements QueryAst {
    public ConstructQueryAst {
        if (constructTemplate == null) {
            constructTemplate = new GroupGraphPatternAst(List.of());
        }
        if (whereClause == null) {
            whereClause = new GroupGraphPatternAst(List.of());
        }
    }
}
