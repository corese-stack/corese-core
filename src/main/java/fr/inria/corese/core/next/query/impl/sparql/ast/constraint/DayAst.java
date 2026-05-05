package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code DAY(dateTime)} in SPARQL 1.1 — §17.4.5.4.
 * Returns the day part of an {@code xsd:dateTime} as an {@code xsd:integer}.
 */
public class DayAst extends AbstractUnaryConstraintAst implements NumericExpressionAst {
    public DayAst(List<TermAst> args) {
        super(args);
    }
}
