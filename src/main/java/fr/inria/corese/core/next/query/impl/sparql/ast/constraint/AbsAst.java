package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code ABS(numeric)} in SPARQL 1.1.
 */
public class AbsAst extends AbstractUnaryConstraintAst implements NumericExpressionAst {
    public AbsAst(List<TermAst> args) {
        super(args);
    }
}
