package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Operator {@code A + A}
 * Addition
 */
public class AddAst extends AbstractChainableOperatorAst implements NumericExpressionAst {
    public AddAst(List<TermAst> args) {
        super(args, AddAst::new);
    }


    @Override
    public String getName() {
        return "+";
    }
}
