package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.VisitableAst;

public record GraphRefAst(IriAst graph, boolean named, boolean all, boolean defaultGraph) implements VisitableAst {
    public GraphRefAst {
        int activeTargets = 0;
        if (graph != null) {
            activeTargets++;
        }
        if (named) {
            activeTargets++;
        }
        if (all) {
            activeTargets++;
        }
        if (defaultGraph) {
            activeTargets++;
        }
        if (activeTargets == 0) {
            throw new QueryEvaluationException("Graph reference does not actually reference any graph");
        }
        if (activeTargets != 1) {
            throw new QueryEvaluationException(
                    "Graph reference must target exactly one of graph, NAMED, ALL, or DEFAULT");
        }
    }

    public GraphRefAst(IriAst graph) {
        this(graph, false, false, false);
    }

    public GraphRefAst(boolean named, boolean all, boolean defaultGraph) {
        this(null, named, all, defaultGraph);
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        if(this.graph != null) {
            this.graph.accept(visitor);
        }
    }
}
