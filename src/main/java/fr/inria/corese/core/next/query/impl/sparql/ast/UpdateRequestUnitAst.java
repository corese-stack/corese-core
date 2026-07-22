package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.VisitableAst;

/**
 * Root interface for all operations related to the SPARQL Update operations listed in <a href="https://www.w3.org/TR/sparql11-update/">SPARQL 1.1 Update</a>.
 */
public sealed interface UpdateRequestUnitAst extends VisitableAst permits LoadRequestAst, ClearRequestAst, CreateRequestAst, DropRequestAst {
}
