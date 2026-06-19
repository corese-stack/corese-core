package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

/**
 * AST node representing a SPARQL {@code MINUS} graph pattern.
 */
public record MinusAst(GroupGraphPatternAst pattern) implements PatternAst {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.pattern.accept(visitor);
    }
}
