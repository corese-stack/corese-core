package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;

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

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.graphRef.accept(visitor);
    }
}