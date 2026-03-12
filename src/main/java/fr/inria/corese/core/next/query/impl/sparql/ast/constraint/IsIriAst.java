package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code isIri(A)} and {@code isUri(A)}
 */
public class IsIriAst extends AbstractUnaryConstraintAst implements BooleanExpressionAst {
    public IsIriAst(TermAst arg) {
        super(arg);
    }
}
