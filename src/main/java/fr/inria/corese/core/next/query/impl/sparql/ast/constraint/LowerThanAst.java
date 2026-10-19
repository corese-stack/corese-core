package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code <}
 */
public class LowerThanAst extends AbstractChainableOperatorAst implements BooleanExpressionAst {
    public LowerThanAst(List<TermAst> args) {
        super(args, LowerThanAst::new);
    }


    @Override
    public String getName() {
        return "<";
    }
}
