package fr.inria.corese.core.next.query.impl.sparql.ast.path;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

public record AlternativePathAst(PathAst left, PathAst right) implements PathAst {
    public AlternativePathAst {
        if (left == null || right == null) {
            throw new IllegalArgumentException("alternative operands are null");
        }
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        left.accept(visitor);
        right.accept(visitor);
    }
}
