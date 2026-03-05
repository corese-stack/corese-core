package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.ArithmeticOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code - A}
 */
public class UnaryMinusAst extends AbstractUnaryOperatorAst implements ArithmeticOperatorAst {
    public UnaryMinusAst(TermAst arg) {
        super(arg);
    }
}
