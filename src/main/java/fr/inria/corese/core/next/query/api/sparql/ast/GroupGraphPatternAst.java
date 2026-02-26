package fr.inria.corese.core.next.query.api.sparql.ast;

import java.util.List;

/**
 * Group graph pattern: { pattern1 pattern2 ... } (WHERE clause content).
 *
 * */
public record GroupGraphPatternAst(List<PatternAst> patterns) {
    public GroupGraphPatternAst {
        patterns = patterns != null ? List.copyOf(patterns) : List.of();
    }
}
