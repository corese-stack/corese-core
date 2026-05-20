package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;

import java.util.ArrayList;
import java.util.List;

public record GraphRefAst(IriAst graph, boolean named, boolean all, boolean defaultGraph) {
    public GraphRefAst {
        if(graph == null && !named && !all && !defaultGraph) {
            throw new QueryEvaluationException("Graph reference does not actually reference any graph");
        }
        if(named && all) {
            throw new QueryEvaluationException("Cannot have a Graph reference that is ALL and NAMED at the same time");
        }
        if(named && defaultGraph) {
            throw new QueryEvaluationException("Cannot have a Graph reference that is NAMED and DEFAULT at the same time");
        }
        if(defaultGraph && all) {
            throw new QueryEvaluationException("Cannot have a Graph reference that is ALL and DEFAULT at the same time");
        }
    }

    public GraphRefAst(IriAst graph) {
        this(graph, false, false, false);
    }

    public GraphRefAst(boolean named, boolean all, boolean defaultGraph) {
        this(null, named, all, defaultGraph);
    }
}
