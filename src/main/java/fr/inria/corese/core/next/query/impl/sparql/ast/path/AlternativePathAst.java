package fr.inria.corese.core.next.query.impl.sparql.ast.path;

public record AlternativePathAst(PathAst left, PathAst right) implements PathAst {
    public AlternativePathAst {
        if (left == null || right == null) {
            throw new IllegalArgumentException("alternative operands are null");
        }
    }
}
