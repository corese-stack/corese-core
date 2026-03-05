package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.ArithmeticOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code A / A}
 */
public class DivideAst extends AbstractBinaryOperatorAst implements ArithmeticOperatorAst {
    public DivideAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
