package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Root interfact for ann abstract syntax trees for operations
 */
public sealed interface QueryAst permits SparqlQueryAst, UpdateRequestAst {
    QueryPrologueAst prologue();
}
