package fr.inria.corese.core.next.query.impl.sparql.ast.path;

public record OneOrMorePathAst(PathAst pathAst) implements PathAst {
    public OneOrMorePathAst {
        if (pathAst == null) throw new IllegalArgumentException("path is null");
    }
}
