package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code SECONDS(dateTime)} in SPARQL 1.1 — §17.4.5.7.
 * Returns the seconds part of an {@code xsd:dateTime} as an {@code xsd:decimal}.
 */
public class SecondsAst extends AbstractUnaryConstraintAst implements NumericExpressionAst {
    public SecondsAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "SECONDS";
    }
}
