package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code A >= A}
 */
public class GreaterOrEqualThanAst extends AbstractBinaryConstraintAst implements BooleanExpressionAst {
    public GreaterOrEqualThanAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
