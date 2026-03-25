package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.data.impl.common.prefix.PrefixHandler;

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
public record ConstructQueryAst(
        ConstructTemplateAst constructTemplate,
        DatasetClauseAst datasetClause,
        GroupGraphPatternAst whereClause,
        SolutionModifierAst solutionModifier,
        PrefixHandler prefixHandler
            ) implements QueryAst {
    public ConstructQueryAst {
        if (constructTemplate == null) {
            constructTemplate = new ConstructTemplateAst(List.of());
        }
        if (datasetClause == null) {
            datasetClause = DatasetClauseAst.none();
        }
        if (whereClause == null) {
            whereClause = new GroupGraphPatternAst(List.of());
        }
        if (solutionModifier == null) {
            solutionModifier = SolutionModifierAst.empty();
        }
        if (prefixHandler == null) {
            prefixHandler = new PrefixHandler(true);
        }
    }

    /**
     * constructor with default prefix handler
     */
    public ConstructQueryAst(ConstructTemplateAst template, DatasetClauseAst datasetClause, GroupGraphPatternAst whereClause, SolutionModifierAst solutionModifier) {
        this(template, datasetClause, whereClause, solutionModifier, null);
    }

    /**
     * constructor with default prefix handler and default solution modifier
     */
    public ConstructQueryAst(ConstructTemplateAst template, GroupGraphPatternAst whereClause) {
        this(template, DatasetClauseAst.none(), whereClause, SolutionModifierAst.empty(), null);
    }
}