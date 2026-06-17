package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code &&}
 */
public class AndAst extends AbstractChainableOperatorAst implements BooleanExpressionAst {
    public AndAst(List<TermAst> args) {
        super(args);
    }

    @Override
    protected TermAst create(List<TermAst> args) {
        return new AndAst(args);
    }

    @Override
    public String getName() {
        return "&&";
    }
}
