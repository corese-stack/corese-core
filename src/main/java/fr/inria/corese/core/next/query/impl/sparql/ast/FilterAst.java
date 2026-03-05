package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Filter clause in a SPARQL query
 * @param operator The operator whose evaluation will make a filter a constraint on the query results
 */
public record FilterAst(BooleanOperatorAst operator) implements PatternAst {
}
