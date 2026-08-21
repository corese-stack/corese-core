package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import java.util.List;
import java.util.Objects;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * SPARQL 1.1 {@code IN}: {@code rdfTerm IN (expression, ...)} (including
 * {@code IN ()}).
 */
public record InAst(TermAst left, List<TermAst> candidates) implements BooleanExpressionAst {

    public InAst {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(candidates, "candidates");
        candidates = List.copyOf(candidates);
    }

    @Override
    public String getName() {
        return "IN";
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.left.accept(visitor);
        this.candidates.forEach(termAst -> termAst.accept(visitor));
    }
}
