package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code CEIL(numeric)} in SPARQL 1.1.
 */
public class CeilAst extends AbstractUnaryConstraintAst implements NumericExpressionAst {
    public CeilAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "CEIL";
    }
}
