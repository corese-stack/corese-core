package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code + A}
 */
public class UnaryPlusAst extends AbstractUnaryConstraintAst implements NumericExpressionAst {
    public UnaryPlusAst(TermAst arg) {
        super(arg);
    }
}
