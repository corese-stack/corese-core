package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.SimpleLiteralOperatorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code lang(A)}
 */
public class LangAst extends AbstractUnaryOperatorAst implements SimpleLiteralOperatorAst {
    public LangAst(TermAst arg) {
        super(arg);
    }
}
