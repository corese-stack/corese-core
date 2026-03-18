package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.HashSet;
import java.util.Set;

public record DatasetClauseAst(Set<IriAst> graphs, Set<IriAst> namedGraphs) {

    public DatasetClauseAst {
        if (graphs == null) {
            graphs = new HashSet<>();
        }
        if (namedGraphs == null) {
            namedGraphs = new HashSet<>();
        }
    }

    public static DatasetClauseAst none() {
        return new DatasetClauseAst(new HashSet<>(), new HashSet<>());
    }
}
