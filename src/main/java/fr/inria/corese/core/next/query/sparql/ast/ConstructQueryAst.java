package fr.inria.corese.core.next.query.sparql.ast;

import java.util.List;

/**
 * Abstract Syntax Tree (AST) representation of a SPARQL {@code CONSTRUCT} query.
 * CONSTRUCT { template } WHERE { pattern }.
 *
 * PREFIX foaf:    <http://xmlns.com/foaf/0.1/>
 * PREFIX vcard:   <http://www.w3.org/2001/vcard-rdf/3.0#>
 * CONSTRUCT   { <http://example.org/person#Alice> vcard:FN ?name }
 * WHERE       { ?x foaf:name ?name }
 *
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
