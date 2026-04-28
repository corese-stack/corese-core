package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code TZ(dateTime)} in SPARQL 1.1 — §17.4.5.9.
 * Returns the timezone part of an {@code xsd:dateTime} as a simple literal.
 * Returns the empty string if the argument has no timezone.
 */
public class TzAst extends AbstractUnaryConstraintAst implements SimpleLiteralExpressionAst {
    public TzAst(List<TermAst> args) {
        super(args);
    }
}
