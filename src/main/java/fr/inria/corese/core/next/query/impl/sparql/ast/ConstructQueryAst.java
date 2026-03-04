package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;

/**
 * Abstract Syntax Tree (AST) representation of a SPARQL {@code CONSTRUCT} query.
 *
 * <p>
 * A CONSTRUCT query creates an RDF graph by applying a template to each
 * solution of the WHERE graph pattern.
 * </p>
 *
 * <p>Example:</p>
 *
 * <pre>{@code
 * PREFIX foaf:  <http://xmlns.com/foaf/0.1/>
 * PREFIX vcard: <http://www.w3.org/2001/vcard-rdf/3.0#>
 *
 * CONSTRUCT {
 *   <http://example.org/person#Alice> vcard:FN ?name
 * }
 * WHERE {
 *   ?x foaf:name ?name
 * }
 * }</pre>
 *
 * <p>
 * For each solution mapping produced by the {@code WHERE} clause,
 * the {@code CONSTRUCT} template is instantiated and the resulting
 * triples are added to the output graph.
 * </p>
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