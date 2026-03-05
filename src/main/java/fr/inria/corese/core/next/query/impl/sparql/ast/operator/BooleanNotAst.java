package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.BooleanOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code !}
 */
public class BooleanNotAst extends AbstractUnaryOperatorAst implements BooleanOperatorAst {
    public BooleanNotAst(TermAst arg) {
        super(arg);
    }
}
