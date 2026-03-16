package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code lang(A)}
 * {@code LANG(literal)}: returns the language tag of a plain literal,
 * or an empty string if the literal has no language tag.
 */
public class LangAst extends AbstractUnaryConstraintAst implements SimpleLiteralExpressionAst {
    public LangAst(TermAst arg) {
        super(arg);
    }
}
