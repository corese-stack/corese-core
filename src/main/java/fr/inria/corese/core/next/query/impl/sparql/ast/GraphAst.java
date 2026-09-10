package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;

/** A named graph pattern, with an IRI or variable graph name. */
public record GraphAst(TermAst name, GroupGraphPatternAst pattern) implements PatternAst {
    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        name.accept(visitor);
        pattern.accept(visitor);
    }
}
