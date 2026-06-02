package fr.inria.corese.core.next.query.impl.sparql.ast.path;

import java.util.List;

public record NegatedPropertySetPathAst(List<PathAst> excluded) implements PathAst {
    public NegatedPropertySetPathAst {
        excluded = excluded != null ? List.copyOf(excluded) : List.of();
    }
}
