package fr.inria.corese.core.next.query.api.sparql.ast;

import java.util.List;

/**
 * AST representation of a SPARQL DESCRIBE query.
 */
public non-sealed interface DescribeQueryAst extends QueryAst {
    List<TermAst> described();
    @Override
    GroupGraphPatternAst whereClause();
}
