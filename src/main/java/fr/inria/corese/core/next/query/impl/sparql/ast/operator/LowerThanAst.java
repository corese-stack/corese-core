package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code <}
 */
public class LowerThanAst extends AbstractBinaryOperatorAst {
    public LowerThanAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
