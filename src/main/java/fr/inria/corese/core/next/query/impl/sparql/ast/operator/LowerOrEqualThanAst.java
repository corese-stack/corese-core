package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.BooleanOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code <=}
 */
public class LowerOrEqualThanAst extends AbstractBinaryOperatorAst implements BooleanOperatorAst {
    public LowerOrEqualThanAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
