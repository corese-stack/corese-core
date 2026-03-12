package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code - A}
 */
public class UnaryMinusAst extends AbstractUnaryConstraintAst implements NumericExpressionAst {
    public UnaryMinusAst(TermAst arg) {
        super(arg);
    }
}
