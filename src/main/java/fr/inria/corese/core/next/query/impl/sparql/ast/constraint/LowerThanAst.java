package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code <}
 */
public class LowerThanAst extends AbstractBinaryConstraintAst implements BooleanExpressionAst {
    public LowerThanAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
