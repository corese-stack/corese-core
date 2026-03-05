package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.BooleanOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code bound(A)}
 */
public class BoundAst extends AbstractUnaryOperatorAst implements BooleanOperatorAst {
    public BoundAst(TermAst arg) {
        super(arg);
    }
}
