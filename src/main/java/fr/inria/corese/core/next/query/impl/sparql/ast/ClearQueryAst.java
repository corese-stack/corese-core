package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Represents the CLEAR queries as defined in the <a href="https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#clear">SPARQL 1.1 recommendation<a/>.
 * @param graphRef targeted graph to be cleared
 * @param silent Determine if the resolution of the query must be resolved silently or not.
 */
public record ClearQueryAst(QueryPrologueAst prologue, GraphRefAst graphRef, boolean silent) implements UpdateQueryUnitAst {
    public ClearQueryAst{
        if(prologue == null) {
            prologue = QueryPrologueAst.empty();
        }
    }

    /**
     * Construct CLEAR query with empty prologue.
     * @param graphRef targeted graph to be cleared
     * @param silentFlag Determine if the resolution of the query must be resolved silently or not.
     */
    public ClearQueryAst(GraphRefAst graphRef, boolean silentFlag) {
        this(QueryPrologueAst.empty(), graphRef, silentFlag);
    }

    /**
     * Construct CLEAR query with empty prologue and a silent flag to false.
     * @param graphRef targeted graph to be cleared
     */
    public ClearQueryAst(GraphRefAst graphRef) {
        this(QueryPrologueAst.empty(), graphRef, false);
    }
}
