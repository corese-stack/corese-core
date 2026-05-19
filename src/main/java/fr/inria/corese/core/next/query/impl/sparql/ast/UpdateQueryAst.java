package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Root interface for all queries related to the SPARQL Update queries listed in <a href="https://www.w3.org/TR/sparql11-update/">SPARQL 1.1 Update</>.
 */
public sealed interface UpdateQueryAst extends QueryAst permits LoadQueryAst {
}
