package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code lang(A)}
 */
public class LangAst extends AbstractUnaryConstraintAst implements SimpleLiteralExpressionAst {
    public LangAst(TermAst arg) {
        super(arg);
    }
}
