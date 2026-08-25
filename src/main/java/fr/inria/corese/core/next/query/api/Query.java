package fr.inria.corese.core.next.query.api;

import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;

import java.time.Duration;

/**
 * A prepared SPARQL query that can be evaluated against a repository.
 *
 * <p>Concrete subtypes are {@link TupleQuery} (SELECT), {@link BooleanQuery} (ASK),
 * and {@link GraphQuery} (CONSTRUCT / DESCRIBE).
 * Bindings, dataset, and timeout are applied on each {@link #evaluate()} call.</p>
 */
public interface Query<T> {

    /**
     * @return the textual representation of this query (e.g., SPARQL string)
     */
    String getQueryString();

    /**
     * Sets the maximum evaluation duration. {@link Duration#ZERO} means no limit.
     *
     * @param timeout non-negative timeout
     * @return this
     * @throws NullPointerException if {@code timeout} is {@code null}
     * @throws IllegalArgumentException if {@code timeout} is negative
     */
    Query<T> setTimeout(Duration timeout);

    /**
     * Returns the configured evaluation timeout.
     *
     * @return a non-negative duration; zero means no limit
     */
    Duration getTimeout();

    /**
     * Evaluates the query against the dataset.
     *
     * @return The result type expected: {@link TupleQueryResult}
     *         for SELECT queries, {@link GraphQueryResult}
     *         for CONSTRUCT/DESCRIBE queries, or {@link Boolean} for ASK queries
     * @throws QueryEvaluationException if an error occurs during query evaluation
     */
    T evaluate() throws QueryEvaluationException;

    /**
     * Sets or replaces an initial variable binding.
     *
     * @param name variable name without a leading {@code ?} or {@code $}
     * @param value non-null RDF value
     * @return this query
     */
    Query<T> setBinding(String name, Value value);

    /** Removes an initial binding when present and returns this query. */
    Query<T> removeBinding(String name);

    /** Removes every initial binding and returns this query. */
    Query<T> clearBindings();

    /**
     * Sets a dataset that replaces the query's {@code FROM}/{@code FROM NAMED}
     * clauses, or clears the override when {@code null}.
     */
    Query<T> setDataset(Dataset dataset);

    /** Returns an immutable snapshot of the current initial bindings. */
    BindingSet getBindings();

    /** Returns the explicit dataset override, or {@code null} when none is set. */
    Dataset getDataset();
}
