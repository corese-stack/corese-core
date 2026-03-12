package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Abstract implementation of {@link BinaryConstraintAst}
 */
public abstract class AbstractBinaryConstraintAst implements BinaryConstraintAst {

    private final TermAst leftArgument;
    private final TermAst rightArgument;

    protected AbstractBinaryConstraintAst(TermAst left, TermAst right) {
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
