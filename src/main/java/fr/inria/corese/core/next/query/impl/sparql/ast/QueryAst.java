package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.sparql.ast.*;


/**
 * Minimal SPARQL query AST (SELECT, ASK, CONSTRUCT, DESCRIBE).
 * Holds the WHERE clause as a group graph pattern; query-specific projection/template can be added later.
 */
public sealed interface QueryAst permits SelectQueryAst {
    GroupGraphPatternAst whereClause();
}