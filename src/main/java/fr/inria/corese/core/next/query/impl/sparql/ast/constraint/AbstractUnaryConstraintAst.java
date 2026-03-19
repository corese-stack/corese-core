package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

public abstract class AbstractUnaryConstraintAst implements UnaryConstraintAst {
    private final TermAst argument;

    protected AbstractUnaryConstraintAst(List<TermAst> args) {
        if(args.size() == 1) {
            this.argument = args.getFirst();
        } else {
            throw new QuerySyntaxException("Too many arguments (" + args.size() + ") for unary operator");
        }
    }

    public TermAst getArgument() {
        return this.argument;
    }
}
