package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code A - A}
 * Subtraction
 */
public class SubtractAst extends AbstractChainableOperatorAst implements NumericExpressionAst {
    public SubtractAst(List<TermAst> args) {
        super(args, SubtractAst::new);
    }


    @Override
    public String getName() {
        return "-";
    }
}
