package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.ArithmeticOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code + A}
 */
public class UnaryPlusAst extends AbstractUnaryOperatorAst implements ArithmeticOperatorAst {
    public UnaryPlusAst(TermAst arg) {
        super(arg);
    }
}
