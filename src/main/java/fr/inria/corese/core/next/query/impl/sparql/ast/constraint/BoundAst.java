package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Operator {@code bound(A)}
 * {@code BOUND(?var)}: returns {@code true} if the variable {@code ?var}
 * is bound (has a value) in the current solution mapping.
 */
public class BoundAst extends AbstractUnaryConstraintAst implements BooleanExpressionAst {
    public BoundAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "BOUND";
    }
}
