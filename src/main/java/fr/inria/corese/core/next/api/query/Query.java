package fr.inria.corese.core.next.api.query;

import fr.inria.corese.core.next.api.query.dataset.Dataset;

/**
 * A query on a repository that can be formulated in one of the supported query languages (for example SPARQL).
 * It should hold kgram.core.Query + ASTQuery
 */

public interface Query {
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
     * @return the query language of this query (typically {@link QueryLanguage})
     */
    QueryLanguage getLanguage();

    Dataset getDataset();

    Query setDataset(Dataset dataset);

    // Execution options
    Query setIncludeInferred(boolean includeInferred);

    Query setTimeout(long timeoutMillis);

    /**
     * @return the type of this query (BOOLEAN / GRAPH / TUPLE)
     */
    QueryType getQueryType();

    Query setMaxExecutionTime(int maxExecutionTimeSeconds);
}
