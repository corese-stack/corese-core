package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;

/**
 * Represents the CREATE operation as defined
 *
 * @param graphRef the named graph to create
 * @param silent   determines if the resolution of the query must be resolved silently or not
 *                 (e.g. ignore the error if the graph already exists)
 */
public record CreateRequestAst(GraphRefAst graphRef, boolean silent) implements UpdateRequestUnitAst {

    public CreateRequestAst {
        if (graphRef == null) {
            throw new QueryEvaluationException("CREATE query must reference a graph");
        }
        if (graphRef.graph() == null) {
            throw new QueryEvaluationException(
                    "CREATE query must target a named graph (CREATE GRAPH <iri>)");
        }
    }

    /**
     * Construct a CREATE query with the silent flag set to false.
     *
     * @param graphRef the named graph to create
     */
    public CreateRequestAst(GraphRefAst graphRef) {
        this(graphRef, false);
    }
}