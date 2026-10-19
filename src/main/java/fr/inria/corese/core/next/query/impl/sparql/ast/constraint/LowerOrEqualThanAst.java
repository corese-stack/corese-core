package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code <=}
 */
public class LowerOrEqualThanAst extends AbstractChainableOperatorAst implements BooleanExpressionAst {
    public LowerOrEqualThanAst(List<TermAst> args) {
        super(args, LowerOrEqualThanAst::new);
    }


    @Override
    public String getName() {
        return "<=";
    }
}
