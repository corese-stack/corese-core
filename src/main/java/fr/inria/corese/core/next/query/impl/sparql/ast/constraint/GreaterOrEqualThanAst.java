package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code A >= A}
 */
public class GreaterOrEqualThanAst extends AbstractChainableOperatorAst implements BooleanExpressionAst {
    public GreaterOrEqualThanAst(List<TermAst> args) {
        super(args, GreaterOrEqualThanAst::new);
    }


    @Override
    public String getName() {
        return ">=";
    }
}
