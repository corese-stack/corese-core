package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

public abstract class AbstractUnaryOperatorAst implements UnaryOperatorAst {
    private TermAst argument;

    protected AbstractUnaryOperatorAst(TermAst arg) {
        this.argument = arg;
    }

    public TermAst getArgument() {
        return this.argument;
    }
}
