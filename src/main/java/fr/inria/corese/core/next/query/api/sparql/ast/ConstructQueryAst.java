package fr.inria.corese.core.next.query.api.sparql.ast;

/**
 * AST representation of a SPARQL CONSTRUCT query.
 */
public non-sealed interface ConstructQueryAst extends QueryAst {
    GroupGraphPatternAst constructTemplate();
    @Override
    GroupGraphPatternAst whereClause();
}
