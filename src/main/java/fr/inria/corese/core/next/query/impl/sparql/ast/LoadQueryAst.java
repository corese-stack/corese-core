package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;

/**
 * Represents the LOAD queries as defined in the <a href="https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#load">SPARQL 1.1 recommendation<a/>.
 * @param prologue Prologue declarations.
 * @param fromClause From clause, set of graphs IRIs
 * @param toClause To clause. Set of Graph IRIs
 * @param silent Determine if the resolution of the query must be resolved silently or not.
 */
public record LoadQueryAst(QueryPrologueAst prologue, GraphRefAst fromClause, GraphRefAst toClause, boolean silent) implements UpdateQueryUnitAst {
    public LoadQueryAst {
        if(fromClause == null) {
            throw new QueryEvaluationException("Load query must have at least an URI to load from");
        }
        if(prologue == null) {
            prologue = QueryPrologueAst.empty();
        }
    }

    /**
     * Construct a LOAD query with an empty prologue.
     * @param fromClause From clause, set of graphs IRIs
     * @param toClause To clause. Set of Graph IRIs
     * @param silentFlag Determine if the resolution of the query must be resolved silently or not.
     */
    public LoadQueryAst(GraphRefAst fromClause, GraphRefAst toClause, boolean silentFlag) {
        this(QueryPrologueAst.empty(), fromClause, toClause, silentFlag);
    }


    /**
     * Construct a LOAD query with an empty prologue and a silent flag to false.
     * @param fromClause From clause, set of graphs IRIs
     * @param toClause To clause. Set of Graph IRIs
     */
    public LoadQueryAst(GraphRefAst fromClause, GraphRefAst toClause) {
        this(QueryPrologueAst.empty(), fromClause, toClause, false);
    }
}
