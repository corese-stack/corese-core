package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code !=}
 */
public class DifferentAst extends AbstractChainableOperatorAst implements BooleanExpressionAst {
    public DifferentAst(List<TermAst> args) {
        super(args);
    }

    @Override
    protected TermAst create(List<TermAst> args) {
        return new DifferentAst(args);
    }

    @Override
    public String getName() {
        return "!=";
    }
}
