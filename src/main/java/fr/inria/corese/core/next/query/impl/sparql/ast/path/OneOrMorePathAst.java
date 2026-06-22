package fr.inria.corese.core.next.query.impl.sparql.ast.path;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

public record OneOrMorePathAst(PathAst pathAst) implements PathAst {
    public OneOrMorePathAst {
        if (pathAst == null) throw new IllegalArgumentException("path is null");
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        if(this.pathAst != null) {
            this.pathAst.accept(visitor);
        }
    }
}
