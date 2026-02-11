package fr.inria.corese.core.next.query.api;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;

/**
 * A query on a repository that can be formulated in one of the supported query languages (for example SPARQL).
 * It should hold kgram.core.Query + ASTQuery
 */

public interface Query<T> extends Operation {

    /**
     * The different types of queries supported by the engine:
     * boolean, graph, and tuple queries.
     */
    enum QueryType {
        /**
         * Boolean queries (e.g., SPARQL ASK) return a single {@code true} / {@code false} result.
         */
        BOOLEAN,

        /**
         * Graph queries (e.g., SPARQL CONSTRUCT / DESCRIBE) return a sequence of RDF triples.
         */
        GRAPH,

        /**
         * Tuple queries (e.g., SPARQL SELECT) return a sequence of variable bindings.
         */
        TUPLE
    }

    /**
     * @return the textual representation of this query (e.g., SPARQL string)
     */
    String getQueryString();

    /**
     * @return the query language of this query (typically {@link fr.inria.corese.core.next.data.api.query.QueryLanguage})
     */
    fr.inria.corese.core.next.data.api.query.QueryLanguage getLanguage();

    // Execution options
    /**
     * Set the execution timeout for the query regarding remote operations (i.e. for SERVICE clauses)
     * @param timeoutMillis time in milliseconds
     * @return this
     */
    Query<T> setTimeout(long timeoutMillis);

    /**
     * @return the type of this query (BOOLEAN / GRAPH / TUPLE)
     */
    QueryType getQueryType();

    /**
     * Evaluates the query against the dataset.
     *
     * @return The result type expected: {@link TupleQueryResult}
     *         for SELECT queries, {@link GraphQueryResult}
     *         for CONSTRUCT/DESCRIBE queries, or {@link Boolean} for ASK queries
     * @throws QueryEvaluationException if an error occurs during query evaluation
     */
    T evaluate() throws QueryEvaluationException;

}
