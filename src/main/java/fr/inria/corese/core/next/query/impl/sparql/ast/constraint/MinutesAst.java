package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code MINUTES(dateTime)} in SPARQL 1.1 — §17.4.5.6.
 * Returns the minutes part of an {@code xsd:dateTime} as an {@code xsd:integer}.
 */
public class MinutesAst extends AbstractUnaryConstraintAst implements NumericExpressionAst {
    public MinutesAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "MINUTES";
    }
}
