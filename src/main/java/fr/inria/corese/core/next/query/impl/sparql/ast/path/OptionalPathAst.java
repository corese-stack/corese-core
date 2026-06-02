package fr.inria.corese.core.next.query.impl.sparql.ast.path;

public record OptionalPathAst(PathAst pathAst) implements PathAst {
    public OptionalPathAst {
        if (pathAst == null) throw new IllegalArgumentException("path is null");
    }
}
