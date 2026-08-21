package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;

/**
 * Represents the DROP operation as defined in the <a href="https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#drop">SPARQL 1.1 recommendation</a>.
 * @param graphRef targeted graph to be dropped
 * @param silent Determine if the resolution of the query must be resolved silently or not.
 */
public record DropRequestAst(GraphRefAst graphRef, boolean silent) implements UpdateRequestUnitAst {
    /**
     * Construct DROP query with a silent flag to false.
     * @param graphRef targeted graph to be dropped
     */
    public DropRequestAst(GraphRefAst graphRef) {
        this(graphRef, false);
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.graphRef.accept(visitor);
    }
}
