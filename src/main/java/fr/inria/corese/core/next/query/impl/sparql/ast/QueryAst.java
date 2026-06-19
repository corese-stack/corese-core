package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.VisitableAst;

/**
 * Root interface for an abstract syntax trees for operation
 */
public sealed interface QueryAst extends VisitableAst permits SparqlQueryAst, UpdateRequestAst {
    QueryPrologueAst prologue();
}
