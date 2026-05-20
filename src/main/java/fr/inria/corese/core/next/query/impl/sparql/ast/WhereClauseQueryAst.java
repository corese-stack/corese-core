package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Represents the AST elements of a query that has a WHERE clause (SELECT, CONSTRUCT, INSERT, DELETE, etc.)
 */
public sealed interface WhereClauseQueryAst permits SparqlQueryAst {
    GroupGraphPatternAst whereClause();
}
