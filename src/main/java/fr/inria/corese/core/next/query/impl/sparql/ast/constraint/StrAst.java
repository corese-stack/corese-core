package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Operator {@code str(A)}
 * {@code STR(term)}: returns the string form of a literal or IRI.
 * For a literal, returns its lexical form; for an IRI, returns its IRI string.
 */
public class StrAst extends AbstractUnaryConstraintAst implements SimpleLiteralExpressionAst {
    public StrAst(List<TermAst> args) {
        super(args);
    }
}
