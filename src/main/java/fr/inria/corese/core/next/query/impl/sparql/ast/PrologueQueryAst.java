package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Represents queries that can be prefixed with a prologue declaration (i.e. Select, Insert, etc.)
 */
public sealed interface PrologueQueryAst permits SparqlQueryAst {
    QueryPrologueAst prologue();
}
