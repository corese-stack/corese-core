package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Operator {@code + A}
 */
public class UnaryPlusAst extends AbstractUnaryConstraintAst implements NumericExpressionAst {
    public UnaryPlusAst(List<TermAst> args) {
        super(args);
    }
}
