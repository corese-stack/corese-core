package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;
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

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.pattern.accept(visitor);
    }
}