package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;
import java.util.Objects;

/**
 * SPARQL 1.1 {@code IN}: {@code rdfTerm IN (expression, ...)} (including {@code IN ()}).
 */
public record InAst(TermAst left, List<TermAst> candidates) implements ConstraintAst, BooleanExpressionAst {

    public InAst {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(candidates, "candidates");
        candidates = List.copyOf(candidates);
    }
}
