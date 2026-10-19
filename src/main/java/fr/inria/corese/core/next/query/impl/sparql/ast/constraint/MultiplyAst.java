package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code A * A}
 */
public class MultiplyAst extends AbstractChainableOperatorAst implements NumericExpressionAst {
    public MultiplyAst(List<TermAst> args) {
        super(args, MultiplyAst::new);
    }


    @Override
    public String getName() {
        return "*";
    }
}
