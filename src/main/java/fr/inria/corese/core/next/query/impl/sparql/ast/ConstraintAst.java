package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.VisitableAst;

/**
 * Root interface for all operators resolving to a term
 */
public non-sealed interface ConstraintAst extends TermAst, VisitableAst {
}
