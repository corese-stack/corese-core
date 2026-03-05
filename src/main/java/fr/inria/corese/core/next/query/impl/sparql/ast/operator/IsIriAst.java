package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.BooleanOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code isIri(A)} and {@code isUri(A)}
 */
public class IsIriAst extends AbstractUnaryOperatorAst implements BooleanOperatorAst {
    public IsIriAst(TermAst arg) {
        super(arg);
    }
}
