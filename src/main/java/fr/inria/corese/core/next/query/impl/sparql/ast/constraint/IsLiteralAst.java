package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code IsLiteral(A)}
 */
public class IsLiteralAst extends AbstractUnaryConstraintAst implements BooleanExpressionAst {
    public IsLiteralAst(TermAst arg) {
        super(arg);
    }
}
