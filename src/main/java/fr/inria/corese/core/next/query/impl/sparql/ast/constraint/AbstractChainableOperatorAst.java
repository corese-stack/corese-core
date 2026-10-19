package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;
import java.util.function.Function;

public abstract class AbstractChainableOperatorAst extends AbstractBinaryConstraintAst {
    protected AbstractChainableOperatorAst(List<TermAst> args, Function<List<TermAst>, TermAst> factory) {
        super(args);
        this.setRightArgument(args.getLast());
        if(args.size() == 2) {
            this.setLeftArgument(args.getFirst());
        } else if(args.size() > 2) {
            List<TermAst> tail = args.subList(0, args.size() - 1);
            this.setLeftArgument(factory.apply(tail));
        } else {
            throw new QuerySyntaxException("Unexpected number of arguments (" + args.size() + ") for binary chainable operator");
        }
    }

}
