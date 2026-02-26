package fr.inria.corese.core.next.query.sparql.ast;

import java.util.List;

/**
 * Abstract Syntax Tree (AST) representation of a SPARQL {@code ASK} query.
 * ASK WHERE { pattern } returns a boolean.
 *
 * PREFIX foaf:    <http://xmlns.com/foaf/0.1/>
 * ASK  { ?x foaf:name  "Alice" ;
 *           foaf:mbox  <mailto:alice@work.example> }
 *
 */
public record AskQueryAst(GroupGraphPatternAst whereClause) implements QueryAst {
    public AskQueryAst {
        if (whereClause == null) {
            whereClause = new GroupGraphPatternAst(List.of());
        }
    }
}
