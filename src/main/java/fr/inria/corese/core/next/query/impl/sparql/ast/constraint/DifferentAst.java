package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code !=}
 */
public class DifferentAst extends AbstractBinaryConstraintAst implements BooleanExpressionAst {
    public DifferentAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
