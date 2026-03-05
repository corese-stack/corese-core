package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code >}
 */
public class GreaterThanAst extends AbstractBinaryOperatorAst {
    public GreaterThanAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
