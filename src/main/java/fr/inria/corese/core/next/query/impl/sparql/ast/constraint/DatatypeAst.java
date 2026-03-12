package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.IriOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code datatype(A)}
 */
public class DatatypeAst extends AbstractUnaryConstraintAst implements IriOperatorAst {
    public DatatypeAst(TermAst arg) {
        super(arg);
    }
}
