package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code TIMEZONE(dateTime)} in SPARQL 1.1 — §17.4.5.8.
 * Returns the timezone part of an {@code xsd:dateTime} as an {@code xsd:dayTimeDuration}.
 * Raises an error if the argument has no timezone.
 */
public class TimezoneAst extends AbstractUnaryConstraintAst implements XsdDayTimeDurationExpressionAst {
    public TimezoneAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "TIMEZONE";
    }
}
