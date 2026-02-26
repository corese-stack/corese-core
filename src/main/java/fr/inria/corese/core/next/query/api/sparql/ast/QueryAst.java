package fr.inria.corese.core.next.query.api.sparql.ast;

/**
 * Minimal SPARQL query AST (e.g. SELECT ... WHERE { ... }).
 * Holds the WHERE clause as a group graph pattern; SELECT/projection can be added later.
 */
public sealed interface QueryAst permits SelectQueryAst {
    GroupGraphPatternAst whereClause();
}