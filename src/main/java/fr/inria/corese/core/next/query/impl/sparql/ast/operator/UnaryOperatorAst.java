package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Interface for AST elements using one term as argument
 */
public interface UnaryOperatorAst {
    TermAst getArgument();
}
