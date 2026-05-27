package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Represents the CLEAR operations as defined in the <a href="https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#clear">SPARQL 1.1 recommendation<a/>.
 * @param graphRef targeted graph to be cleared
 * @param silent Determine if the resolution of the query must be resolved silently or not.
 */
public record ClearRequestAst(GraphRefAst graphRef, boolean silent) implements UpdateRequestUnitAst {
    public ClearRequestAst {
    }

    /**
     * Construct CLEAR query with empty prologue and a silent flag to false.
     * @param graphRef targeted graph to be cleared
     */
    public ClearRequestAst(GraphRefAst graphRef) {
        this( graphRef, false);
    }
}
