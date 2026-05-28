package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Root interface for an abstract syntax trees for operation
 */
public sealed interface QueryAst extends VisitableAst permits SparqlQueryAst, UpdateRequestAst {
    QueryPrologueAst prologue();
}
