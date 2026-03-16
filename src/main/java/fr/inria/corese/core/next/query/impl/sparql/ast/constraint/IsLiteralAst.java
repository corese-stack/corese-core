package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code IsLiteral(A)}
 * {@code isLiteral(term)}: returns {@code true} if {@code term} is a literal.
 */
public class IsLiteralAst extends AbstractUnaryConstraintAst implements BooleanExpressionAst {
    public IsLiteralAst(TermAst arg) {
        super(arg);
    }
}
