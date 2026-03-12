package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code !}
 */
public class BooleanNotAst extends AbstractUnaryConstraintAst implements BooleanExpressionAst {
    public BooleanNotAst(TermAst arg) {
        super(arg);
    }
}
