package fr.inria.corese.core.next.query.impl.sparql.ast.path;

public record SequencePathAst(PathAst left, PathAst right) implements PathAst {
    public SequencePathAst {
        if (left == null || right == null) {
            throw new IllegalArgumentException("sequence operands are null");
        }
    }
}
