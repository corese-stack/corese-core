package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code ||}
 */
public class OrAst extends AbstractBinaryConstraintAst implements BooleanExpressionAst {
    public OrAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
