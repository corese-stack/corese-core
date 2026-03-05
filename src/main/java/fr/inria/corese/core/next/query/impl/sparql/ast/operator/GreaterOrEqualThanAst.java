package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.BooleanOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code A >= A}
 */
public class GreaterOrEqualThanAst extends AbstractBinaryOperatorAst implements BooleanOperatorAst {
    public GreaterOrEqualThanAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
