package fr.inria.corese.core.next.query.impl.sparql.ast;

public sealed interface QueryAst permits SparqlQueryAst, UpdateQueryUnitAst {
    QueryPrologueAst prologue();
}
