package fr.inria.corese.core.next.query.api;

import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;

/**
 * A prepared SPARQL query that can be evaluated against a repository.
 *
 * <p>Concrete subtypes are {@link TupleQuery} (SELECT), {@link BooleanQuery} (ASK),
 * and {@link GraphQuery} (CONSTRUCT / DESCRIBE).
 * All evaluation state (bindings, dataset, timeout) is carried by the
 * {@link Operation} supertype and propagated on each {@link #evaluate()} call.</p>
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
     * @return the query language of this query (typically {@link QueryLanguage})
     */
    QueryLanguage getLanguage();

    /**
     * Sets a fine-grained execution timeout for this specific query, expressed in milliseconds.
     *
     * <p>When both this value and {@link Operation#setMaxExecutionTime(int)} are set,
     * the shorter of the two limits is enforced. A value of {@code 0} disables this
     * query-level timeout.</p>
     *
     * @param timeoutMillis maximum evaluation time in milliseconds; 0 means no limit
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

    @Override
    Query<T> setBinding(String name, Value value);

    @Override
    Query<T> removeBinding(String name);

    @Override
    Query<T> clearBindings();

    @Override
    Query<T> setDataset(Dataset dataset);

    @Override
    Query<T> setIncludeInferred(boolean includeInferred);

    @Override
    Query<T> setMaxExecutionTime(int maxExecutionTimeSeconds);
}
