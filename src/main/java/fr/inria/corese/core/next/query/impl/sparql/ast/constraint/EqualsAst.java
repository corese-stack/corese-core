package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code =}
 */
public class EqualsAst extends AbstractChainableOperatorAst implements BooleanExpressionAst {
    public EqualsAst(List<TermAst> args) {
        super(args, EqualsAst::new);
    }


    @Override
    public String getName() {
        return "=";
    }
}
