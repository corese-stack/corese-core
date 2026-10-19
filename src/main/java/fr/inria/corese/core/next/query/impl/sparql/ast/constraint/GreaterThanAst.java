package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code A > A}
 */
public class GreaterThanAst extends AbstractChainableOperatorAst implements BooleanExpressionAst {
    public GreaterThanAst(List<TermAst> args) {
        super(args, GreaterThanAst::new);
    }


    @Override
    public String getName() {
        return ">";
    }
}
