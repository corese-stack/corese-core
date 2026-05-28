package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;
import java.util.Objects;

/**
 * Abstract implementation of {@link BinaryConstraintAst}
 */
public abstract class AbstractBinaryConstraintAst implements BinaryConstraintAst {

    private TermAst leftArgument;
    private TermAst rightArgument;

    protected AbstractBinaryConstraintAst(List<TermAst> args) {
        Objects.requireNonNull(args);
    }

    @Override
    public TermAst getLeftArgument() {
        return this.leftArgument;
    }

    protected void setLeftArgument(TermAst arg) {
        this.leftArgument = arg;
    }

    @Override
    public TermAst getRightArgument() {
        return this.rightArgument;
    }

    protected void setRightArgument(TermAst arg) {
        this.rightArgument = arg;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.leftArgument.accept(visitor);
        this.rightArgument.accept(visitor);
    }

}
