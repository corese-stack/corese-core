package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.BooleanOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code isBlank(A)}
 */
public class IsBlankAst extends AbstractUnaryOperatorAst implements BooleanOperatorAst {
    public IsBlankAst(TermAst arg) {
        super(arg);
    }
}
