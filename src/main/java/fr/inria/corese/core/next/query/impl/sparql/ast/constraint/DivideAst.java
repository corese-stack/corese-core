package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code A / A}
 */
public class DivideAst extends AbstractChainableOperatorAst implements NumericExpressionAst {
    public DivideAst(List<TermAst> args) {
        super(args, DivideAst::new);
    }


    @Override
    public String getName() {
        return "/";
    }
}
