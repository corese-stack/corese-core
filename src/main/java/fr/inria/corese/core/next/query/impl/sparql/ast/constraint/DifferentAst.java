package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code !=}
 */
public class DifferentAst extends AbstractChainableOperatorAst implements BooleanExpressionAst {
    public DifferentAst(List<TermAst> args) {
        super(args, DifferentAst::new);
    }


    @Override
    public String getName() {
        return "!=";
    }
}
