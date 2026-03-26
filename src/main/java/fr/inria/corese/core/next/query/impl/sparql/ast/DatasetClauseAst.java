package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.LinkedHashSet;
import java.util.Set;

public record DatasetClauseAst(Set<IriAst> graphs, Set<IriAst> namedGraphs) {

    public DatasetClauseAst {
        graphs = graphs == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(graphs));
        namedGraphs = namedGraphs == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(namedGraphs));
    }

    public static DatasetClauseAst none() {
        return new DatasetClauseAst(Set.of(), Set.of());
    }
}
