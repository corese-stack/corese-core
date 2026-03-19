package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Interface for AST elements using one term as argument
 */
public interface UnaryConstraintAst extends ConstraintAst {
    TermAst getArgument();
}
