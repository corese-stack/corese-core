package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractUnlimitedArgumentsFunctionAst implements UnlimitedArgumentsFunctionAst {

    private List<TermAst> arguments = new ArrayList<>();

    public AbstractUnlimitedArgumentsFunctionAst(List<TermAst> arguments) {
        this.arguments = arguments;
    }

    @Override
    public List<TermAst> arguments() {
        return arguments;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.arguments.forEach(termAst -> {
            termAst.accept(visitor);
        });
    }
}
