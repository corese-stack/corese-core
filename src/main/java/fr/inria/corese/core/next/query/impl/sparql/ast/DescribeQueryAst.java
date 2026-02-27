package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;

/**
 * Abstract Syntax Tree (AST) representation of a SPARQL {@code DESCRIBE} query.
 * DESCRIBE (var|uri)* WHERE { pattern } or DESCRIBE (var|uri)*.
 *
 * DESCRIBE <http://example.org/>
 *
 * PREFIX foaf:   <http://xmlns.com/foaf/0.1/>
 * DESCRIBE ?x
 * WHERE    { ?x foaf:mbox <mailto:alice@org> }
 *
 */
public record DescribeQueryAst(List<TermAst> described, GroupGraphPatternAst whereClause) implements QueryAst {
    public DescribeQueryAst {
        described = described != null ? List.copyOf(described) : List.of();
        if (whereClause == null) {
            whereClause = new GroupGraphPatternAst(List.of());
        }
    }
}
