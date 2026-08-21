package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

public abstract class AbstractUnlimitedArgumentsFunctionAst implements UnlimitedArgumentsFunctionAst {

    private final List<TermAst> arguments;

    protected AbstractUnlimitedArgumentsFunctionAst(List<TermAst> arguments) {
        this.arguments = arguments != null ? List.copyOf(arguments) : List.of();
    }

    @Override
    public List<TermAst> arguments() {
        return arguments;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        for (TermAst termAst : arguments) {
            if (termAst != null) {
                termAst.accept(visitor);
            }
        }
    }
}
