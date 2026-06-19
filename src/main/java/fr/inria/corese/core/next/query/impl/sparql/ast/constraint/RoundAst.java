package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code ROUND(numeric)} in SPARQL 1.1.
 */
public class RoundAst extends AbstractUnaryConstraintAst implements NumericExpressionAst {
    public RoundAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "ROUND";
    }
}
