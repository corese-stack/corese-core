package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code =}
 */
public class EqualsAst extends AbstractChainableOperatorAst implements BooleanExpressionAst {
    public EqualsAst(List<TermAst> args) {
        super(args);
    }

    @Override
    protected TermAst create(List<TermAst> args) {
        return new EqualsAst(args);
    }

    @Override
    public String getName() {
        return "=";
    }
}
