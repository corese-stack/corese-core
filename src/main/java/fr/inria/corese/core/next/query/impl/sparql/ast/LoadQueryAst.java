package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents the LOAD queries as defined in the <a href="https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#load">SPARQL 1.1 recommendation<a/>.
 * @param fromClause From clause, set of graphs IRIs
 * @param toClause To clause. Set of Graph IRIs
 * @param silent Determine if the resolution of the query must be resolved silently or not.
 */
public record LoadQueryAst(GraphRefAst fromClause, GraphRefAst toClause, boolean silent) implements UpdateQueryAst {
    public LoadQueryAst {
        if(fromClause == null) {
            throw new QueryEvaluationException("Load query must have at least an URI to load from");
        }
    }

    public LoadQueryAst(GraphRefAst fromClause, GraphRefAst toClause) {
        this(fromClause, toClause, false);
    }
}
