package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;

/**
 * Operator {@code NOT EXISTS { ... }} in SPARQL 1.1 FILTER
 */
public record NotExistsAst(GroupGraphPatternAst pattern) implements ConstraintAst {
}