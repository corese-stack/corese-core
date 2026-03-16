package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code A + A}
 * Addition
 */
public class AddAst extends AbstractBinaryConstraintAst implements NumericExpressionAst {
    public AddAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
