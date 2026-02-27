package fr.inria.corese.core.next.query.api.sparql.ast;

/**
 * AST representation of a SPARQL ASK query.
 */
public non-sealed interface AskQueryAst extends QueryAst {
    @Override
    GroupGraphPatternAst whereClause();
}
