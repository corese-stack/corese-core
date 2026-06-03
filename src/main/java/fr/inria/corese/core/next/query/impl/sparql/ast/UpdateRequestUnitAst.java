package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Root interface for all operations related to the SPARQL Update operations listed in <a href="https://www.w3.org/TR/sparql11-update/">SPARQL 1.1 Update</>.
 */
public sealed interface UpdateRequestUnitAst permits LoadRequestAst, ClearRequestAst {
}
