package fr.inria.corese.core.next.query.impl.sparql.ast.path;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;

public record OptionalPathAst(PathAst pathAst) implements PathAst {
    public OptionalPathAst {
        if (pathAst == null) throw new IllegalArgumentException("path is null");
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        pathAst.accept(visitor);
    }
}
