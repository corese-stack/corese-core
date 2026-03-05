package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.ArithmeticOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code A - A}
 */
public class SubtractAst extends AbstractBinaryOperatorAst implements ArithmeticOperatorAst {
    public SubtractAst(TermAst left, TermAst right) {
        super(left, right);
    }
}
