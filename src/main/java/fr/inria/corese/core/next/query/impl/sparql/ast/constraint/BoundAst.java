package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code bound(A)}
 */
public class BoundAst extends AbstractUnaryConstraintAst implements BooleanExpressionAst {
    public BoundAst(TermAst arg) {
        super(arg);
    }
}
