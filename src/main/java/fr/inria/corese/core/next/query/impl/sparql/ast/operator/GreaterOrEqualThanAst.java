package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code >=}
 */
public class GreaterOrEqualThanAst extends AbstractBinaryOperatorAst {
    public GreaterOrEqualThanAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
