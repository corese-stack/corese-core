package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

public abstract class AbstractChainableOperatorAst extends AbstractBinaryConstraintAst {
    protected AbstractChainableOperatorAst(List<TermAst> args) {
        super(args);
        this.setRightArgument(args.getLast());
        if(args.size() == 2) {
            this.setLeftArgument(args.getFirst());
        } else if(args.size() > 2) {
            List<TermAst> tail = args.subList(0, args.size() - 1);
            this.setLeftArgument(this.create(tail));
        } else {
            throw new QuerySyntaxException("Unexpected number of arguments (" + args.size() + ") for binary chainable operator");
        }
    }

    protected abstract TermAst create(List<TermAst> args);
}
