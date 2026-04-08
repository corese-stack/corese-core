package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import java.util.List;

/**
 * Function {@code COALESCE(expr1, expr2, ...)} in SPARQL 1.1
 * Returns the first argument that evaluates without error.
 */
public record CoalesceAst(List<TermAst> arguments) implements ConstraintAst {}