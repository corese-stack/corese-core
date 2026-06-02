package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Operator {@code A + A}
 * Addition
 */
public class AddAst extends AbstractChainableOperatorAst implements NumericExpressionAst {
    public AddAst(List<TermAst> args) {
        super(args);
    }

    @Override
    protected TermAst create(List<TermAst> args) {
        return new AddAst(args);
    }

    @Override
    public String getName() {
        return "+";
    }
}
