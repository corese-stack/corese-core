package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.BooleanOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code IsLiteral(A)}
 */
public class IsLiteralAst extends AbstractUnaryOperatorAst implements BooleanOperatorAst {
    public IsLiteralAst(TermAst arg) {
        super(arg);
    }
}
