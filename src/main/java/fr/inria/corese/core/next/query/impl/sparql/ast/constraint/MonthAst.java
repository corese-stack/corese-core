package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code MONTH(dateTime)} in SPARQL 1.1 — §17.4.5.3.
 * Returns the month part of an {@code xsd:dateTime} as an {@code xsd:integer}.
 */
public class MonthAst extends AbstractUnaryConstraintAst implements NumericExpressionAst {
    public MonthAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "MONTH";
    }
}
