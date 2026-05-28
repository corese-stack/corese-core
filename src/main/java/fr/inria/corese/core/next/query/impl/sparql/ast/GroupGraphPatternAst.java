package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

import java.util.List;

/**
 * Group graph pattern: { pattern1 pattern2 ... } (WHERE clause content).
 *
 * */
public record GroupGraphPatternAst(List<PatternAst> patterns) implements PatternAst {
    public GroupGraphPatternAst {
        patterns = patterns != null ? List.copyOf(patterns) : List.of();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.patterns.forEach(patternAst -> {
            patternAst.accept(visitor);
        });
    }
}