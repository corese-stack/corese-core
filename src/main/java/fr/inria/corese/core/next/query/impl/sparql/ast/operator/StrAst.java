package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.SimpleLiteralOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code str(A)}
 */
public class StrAst extends AbstractUnaryOperatorAst implements SimpleLiteralOperatorAst {
    public StrAst(TermAst arg) {
        super(arg);
    }
}
