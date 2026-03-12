package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code A - A}
 */
public class SubtractAst extends AbstractBinaryConstraintAst implements NumericExpressionAst {
    public SubtractAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
