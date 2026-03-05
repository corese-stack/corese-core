package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.BooleanOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code A > A}
 */
public class GreaterThanAst extends AbstractBinaryOperatorAst implements BooleanOperatorAst {
    public GreaterThanAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
