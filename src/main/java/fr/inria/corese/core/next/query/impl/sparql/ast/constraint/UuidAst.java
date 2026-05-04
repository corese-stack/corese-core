package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;

/**
 * Function {@code UUID()} in SPARQL 1.1
 * Returns a fresh IRI from the UUID URN scheme.
 */
public record UuidAst() implements ConstraintAst {}