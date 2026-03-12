package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code str(A)}
 */
public class StrAst extends AbstractUnaryConstraintAst implements SimpleLiteralExpressionAst {
    public StrAst(TermAst arg) {
        super(arg);
    }
}
