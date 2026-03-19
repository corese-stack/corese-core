package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Interface for AST elements using two terms as arguments (likely to create a cartesian production operations during query evaluation)
 */
public interface BinaryConstraintAst extends ConstraintAst {

    TermAst getLeftArgument();
    TermAst getRightArgument();
}
