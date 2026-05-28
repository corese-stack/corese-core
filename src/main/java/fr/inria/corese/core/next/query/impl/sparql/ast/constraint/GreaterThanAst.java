package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Operator {@code A > A}
 */
public class GreaterThanAst extends AbstractChainableOperatorAst implements BooleanExpressionAst {
    public GreaterThanAst(List<TermAst> args) {
        super(args);
    }

    @Override
    protected TermAst create(List<TermAst> args) {
        return new GreaterThanAst(args);
    }

    @Override
    public String getName() {
        return ">";
    }
}
