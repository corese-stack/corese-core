package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;

public record GroupByAst(List<TermAst> expressions) {
    public GroupByAst {
        expressions = expressions != null ? List.copyOf(expressions) : List.of();
    }

    public boolean isEmpty() {
        return expressions.isEmpty();
    }
}
