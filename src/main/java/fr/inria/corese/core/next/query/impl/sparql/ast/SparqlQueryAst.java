package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Minimal SPARQL query AST (SELECT, ASK, CONSTRUCT, DESCRIBE).
 * Holds the WHERE clause as a group graph pattern; query-specific projection/template can be added later.
 */
public sealed interface SparqlQueryAst extends QueryAst permits AskQueryAst, ConstructQueryAst, DescribeQueryAst, SelectQueryAst {
    QueryPrologueAst prologue();
    DatasetClauseAst datasetClause();
    GroupGraphPatternAst whereClause();
}