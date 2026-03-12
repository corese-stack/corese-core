package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code A > A}
 */
public class GreaterThanAst extends AbstractBinaryConstraintAst implements BooleanExpressionAst {
    public GreaterThanAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
