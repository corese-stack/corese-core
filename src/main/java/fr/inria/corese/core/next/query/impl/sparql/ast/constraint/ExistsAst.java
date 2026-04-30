package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;

import java.util.Objects;

/**
 * SPARQL 1.1 {@code EXISTS { pattern }} in a {@code FILTER} (and elsewhere an expression is allowed).
 * Evaluates whether the pattern has a solution given the current binding.
 */
public record ExistsAst(GroupGraphPatternAst pattern) implements ConstraintAst, BooleanExpressionAst {

    public ExistsAst {
        Objects.requireNonNull(pattern, "pattern");
    }
}