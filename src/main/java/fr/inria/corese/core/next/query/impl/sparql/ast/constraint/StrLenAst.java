package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * Function {@code STRLEN(string)} in SPARQL 1.1
 * Returns the length of a string.
 */
public record StrLenAst(TermAst argument) implements ConstraintAst {}