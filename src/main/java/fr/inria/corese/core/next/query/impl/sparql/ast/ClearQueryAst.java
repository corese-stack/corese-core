package fr.inria.corese.core.next.query.impl.sparql.ast;

public record ClearQueryAst(GraphRefAst graphRef, boolean silent) implements UpdateQueryAst {
}
