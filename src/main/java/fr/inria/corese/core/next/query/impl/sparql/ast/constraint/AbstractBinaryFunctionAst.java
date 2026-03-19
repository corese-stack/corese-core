package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

public class AbstractBinaryFunctionAst extends AbstractBinaryConstraintAst {
    protected AbstractBinaryFunctionAst(List<TermAst> args) {
        super(args);
        this.setLeftArgument(args.getFirst());
        this.setRightArgument(args.getLast());
    }
}
