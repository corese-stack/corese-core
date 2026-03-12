package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

public abstract class AbstractUnaryConstraintAst implements UnaryConstraintAst {
    private TermAst argument;

    protected AbstractUnaryConstraintAst(TermAst arg) {
        this.argument = arg;
    }

    public TermAst getArgument() {
        return this.argument;
    }
}
