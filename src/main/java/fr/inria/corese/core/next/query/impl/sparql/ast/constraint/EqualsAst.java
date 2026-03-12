package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code =}
 */
public class EqualsAst extends AbstractBinaryConstraintAst implements BooleanExpressionAst {
    public EqualsAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
