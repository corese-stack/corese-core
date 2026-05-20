package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Constructor functions for Graph references
 */
public class GraphRefAsts {

    public static GraphRefAst named() {
        return new GraphRefAst(true, false, false);
    }

    public static GraphRefAst all() {
        return new GraphRefAst(false, true, false);
    }

    public static GraphRefAst defaultGraph() {
        return new GraphRefAst(false, false, true);
    }

    public static GraphRefAst graph(IriAst graphIri) {
        return new GraphRefAst(graphIri);
    }
}
