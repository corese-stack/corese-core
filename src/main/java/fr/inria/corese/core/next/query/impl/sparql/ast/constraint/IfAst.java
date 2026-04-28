package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Function {@code IF(condition, thenExpr, elseExpr)} in SPARQL 1.1
 * Evaluates condition; if true returns thenExpr, otherwise elseExpr.
 */
public record IfAst(TermAst condition, TermAst thenExpr, TermAst elseExpr) implements ConstraintAst {}