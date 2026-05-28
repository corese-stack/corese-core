package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;

import java.util.Objects;

/**
 * SPARQL 1.1 {@code NOT EXISTS { pattern }} in a {@code FILTER}.
 * Semantically equivalent to applying {@code fn:not} to {@link ExistsAst} on the same pattern.
 */
public record NotExistsAst(GroupGraphPatternAst pattern) implements BooleanExpressionAst {

    public NotExistsAst {
        Objects.requireNonNull(pattern, "pattern");
    }

    @Override
    public String getName() {
        return "NOT EXISTS";
    }
}