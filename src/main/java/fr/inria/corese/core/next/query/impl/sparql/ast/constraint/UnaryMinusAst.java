package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Operator {@code - A}
 */
public class UnaryMinusAst extends AbstractUnaryConstraintAst implements NumericExpressionAst {
    public UnaryMinusAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "-";
    }
}
