package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code isBlank(A)}
 * {@code isBlank(term)}: returns {@code true} if {@code term} is a blank node.
 */
public class IsBlankAst extends AbstractUnaryConstraintAst implements BooleanExpressionAst {
    public IsBlankAst(TermAst arg) {
        super(arg);
    }
}
