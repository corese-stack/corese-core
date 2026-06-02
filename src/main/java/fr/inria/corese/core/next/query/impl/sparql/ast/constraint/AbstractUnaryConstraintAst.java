package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

public abstract class AbstractUnaryConstraintAst implements UnaryConstraintAst {
    private final TermAst argument;

    public AbstractUnaryConstraintAst(TermAst arg) {
        if (arg == null) {
            throw new IllegalArgumentException("arg must be non-null");
        }
        this.argument = arg;
    }

    public AbstractUnaryConstraintAst(List<TermAst> args) {
        if(args.size() == 1) {
            this.argument = args.getFirst();
        } else {
            throw new QuerySyntaxException("Too many arguments (" + args.size() + ") for unary operator");
        }
    }

    public TermAst argument() {
        return this.argument;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.argument.accept(visitor);
    }
}
