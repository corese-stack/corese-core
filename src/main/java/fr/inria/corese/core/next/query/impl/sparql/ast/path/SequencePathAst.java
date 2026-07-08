package fr.inria.corese.core.next.query.impl.sparql.ast.path;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

public record SequencePathAst(PathAst left, PathAst right) implements PathAst {
    public SequencePathAst {
        if (left == null || right == null) {
            throw new IllegalArgumentException("sequence operands are null");
        }
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        left.accept(visitor);
        right.accept(visitor);
    }
}
