package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code HOURS(dateTime)} in SPARQL 1.1 — §17.4.5.5.
 * Returns the hours part of an {@code xsd:dateTime} as an {@code xsd:integer}.
 */
public class HoursAst extends AbstractUnaryConstraintAst implements NumericExpressionAst {
    public HoursAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "HOURS";
    }
}
