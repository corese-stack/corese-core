package fr.inria.corese.core.next.query.impl.sparql.ast.path;

public record InversePathAst(PathAst pathAst) implements PathAst {
    public InversePathAst {
        if (pathAst == null) throw new IllegalArgumentException("path is null");
    }
}
