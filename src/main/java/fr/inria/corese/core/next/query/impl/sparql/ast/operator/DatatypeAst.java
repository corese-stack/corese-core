package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.SimpleLiteralOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code datatype(A)}
 */
public class DatatypeAst extends AbstractUnaryOperatorAst implements SimpleLiteralOperatorAst {
    public DatatypeAst(TermAst arg) {
        super(arg);
    }
}
