package fr.inria.corese.core.next.query.impl.sparql.ast.path;

public record ZeroOrMorePathAst(PathAst pathAst) implements PathAst {
    public ZeroOrMorePathAst {
        if (pathAst == null) throw new IllegalArgumentException("path is null");
    }
}
