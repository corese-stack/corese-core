package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code isBlank(A)}
 * {@code isBlank(term)}: returns {@code true} if {@code term} is a blank node.
 */
public class IsBlankAst extends AbstractUnaryConstraintAst implements BooleanExpressionAst {
    public IsBlankAst(List<TermAst> args) {
        super(args);
    }

    @Override
    public String getName() {
        return "ISBLANK";
    }
}
