package fr.inria.corese.core.next.query.impl.sparql.ast.operator;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Abstract implementation of {@link BinaryOperatorAst}
 */
public abstract class AbstractBinaryOperatorAst implements BinaryOperatorAst {

    private final TermAst leftArgument;
    private final TermAst rightArgument;

    protected AbstractBinaryOperatorAst(TermAst left, TermAst right) {
        this.leftArgument = left;
        this.rightArgument = right;
    }

    @Override
    public TermAst getLeftArgument() {
        return this.leftArgument;
    }

    @Override
    public TermAst getRightArgument() {
        return this.rightArgument;
    }
}
