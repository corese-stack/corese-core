package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;

public record GraphRefAst(IriAst graph, boolean named, boolean all, boolean defaultGraph) {
    public GraphRefAst {
        if(graph == null && !named && !all && !defaultGraph) {
            throw new QueryEvaluationException("Graph reference does not actually reference any graph");
        }
        if((!((graph != null) ^ named ^ all ^ defaultGraph)) || (named && all && defaultGraph)) {
            throw new QueryEvaluationException("Cannot have a Graph reference that is both a specific graph and/or ALL and/or NAMED and/or default at the same time");
        }
    }

    public GraphRefAst(IriAst graph) {
        this(graph, false, false, false);
    }

    public GraphRefAst(boolean named, boolean all, boolean defaultGraph) {
        this(null, named, all, defaultGraph);
    }
}
