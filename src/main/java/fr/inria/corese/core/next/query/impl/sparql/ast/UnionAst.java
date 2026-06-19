package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

/**
 * AST node representing a SPARQL {@code UNION} of two graph patterns.
 */
public record UnionAst(GroupGraphPatternAst left, GroupGraphPatternAst right) implements PatternAst {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.left.accept(visitor);
        this.right.accept(visitor);
    }
}