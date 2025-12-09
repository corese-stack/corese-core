package fr.inria.corese.core.next.api.dataset;

import java.util.Set;

/**
 * Represents the dataset to be used for evaluating a SPARQL query.
 * <p>
 * A SPARQL dataset consists of:
 * <ul>
 *     <li>a collection of <b>default graphs</b> (FROM clauses)</li>
 *     <li>a collection of <b>named graphs</b> (FROM NAMED clauses)</li>
 * </ul>
 * The dataset restricts which graphs are visible during query execution and
 * determines the active default graph and available named graphs for GRAPH patterns.
 * <p>
 *
 * <h2>Usage</h2>
 * A Dataset instance may be supplied at:
 * <ul>
 *     <li>the {@code RepositoryConnection} level (connection-wide dataset)</li>
 *     <li>or for an individual {@code Query} (query-specific dataset)</li>
 * </ul>
 * Query-level configuration always overrides the connection-level dataset.
 *
 * <h2>Mutability</h2>
 * Implementations should treat the {@code addDefaultGraph()},
 * {@code addNamedGraph()}, and {@code clear()} methods as
 * <b>functional updates</b>, returning the same Dataset instance when possible,
 * or a modified immutable clone depending on the implementation strategy.
 */

public interface Dataset {

    /**
     * Returns the set of graph IRIs that constitute the <b>default graph</b>
     * for query evaluation. Each URI corresponds to a graph that will be merged
     * into the active default graph defined by the SPARQL FROM clause.
     *
     * @return an immutable or modifiable {@code Set} of graph IRIs.
     */
    Set<String> getDefaultGraphs();

    /**
     * Returns the set of named graph IRIs that are available for query execution.
     * These graphs correspond to SPARQL {@code FROM NAMED} clauses and are
     * accessible through GRAPH patterns.
     *
     * @return an immutable or modifiable {@code Set} of graph IRIs.
     */
    Set<String> getNamedGraphs();

    /**
     * Adds a graph IRI to the list of default graphs (FROM clause).
     * <p>
     * Implementations may return the same Dataset instance or a modified clone.
     *
     * @param uri the IRI of the graph to add as a default graph.
     * @return this Dataset instance (or a new instance) with the change applied.
     */
    Dataset addDefaultGraph(String uri);

    /**
     * Adds a graph IRI to the list of named graphs (FROM NAMED clause).
     * <p>
     * Implementations may return the same Dataset instance or a modified clone.
     *
     * @param uri the IRI of the named graph.
     * @return this Dataset instance (or a new instance) with the change applied.
     */
    Dataset addNamedGraph(String uri);

    /**
     * Removes all default graphs and named graphs, resulting in an empty dataset.
     * <p>
     * Equivalent to clearing all FROM and FROM NAMED declarations.
     *
     * @return this Dataset instance (or a new instance) cleared of all graphs.
     */
    Dataset clear();
}