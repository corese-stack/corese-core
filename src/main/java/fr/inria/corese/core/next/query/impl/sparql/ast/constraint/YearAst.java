package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code YEAR(dateTime)} in SPARQL 1.1 — §17.4.5.2.
 * Returns the year part of an {@code xsd:dateTime} as an {@code xsd:integer}.
 */
public class YearAst extends AbstractUnaryConstraintAst implements NumericExpressionAst {
    public YearAst(List<TermAst> args) {
        super(args);
    }
}
