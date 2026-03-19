package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Operator {@code ||}
 */
public class OrAst extends AbstractChainableOperatorAst implements BooleanExpressionAst {
    public OrAst(List<TermAst> args) {
        super(args);
    }

    @Override
    protected TermAst create(List<TermAst> args) {
        return new OrAst(args);
    }
}
