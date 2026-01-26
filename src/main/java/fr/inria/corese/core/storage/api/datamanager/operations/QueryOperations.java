package fr.inria.corese.core.storage.api.datamanager.operations;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.storage.api.datamanager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.datamanager.support.model.EdgePattern;

import java.util.stream.Stream;

/**
 * Query operations for reading edges.
 * Provides modern Stream-based API for querying the graph.
 */
public interface QueryOperations {

    /**
     * Queries edges matching the given pattern.
     *
     * @param pattern Edge pattern to match
     * @return Stream of matching edges (must be closed)
     * @throws DataManagerException     if query fails
     * @throws IllegalArgumentException if pattern is null
     */
    Stream<Edge> query(EdgePattern pattern) throws DataManagerException;

    /**
     * Counts edges matching the given pattern.
     * More efficient than query().count() as it doesn't need to load edges.
     *
     * @param pattern Edge pattern to match
     * @return Number of matching edges
     * @throws DataManagerException     if count fails
     * @throws IllegalArgumentException if pattern is null
     */
    long count(EdgePattern pattern) throws DataManagerException;

    /**
     * Checks if at least one edge matches the given pattern.
     * More efficient than query().findAny() as it can stop after first match.
     *
     * @param pattern Edge pattern to match
     * @return true if at least one edge matches
     * @throws DataManagerException     if check fails
     * @throws IllegalArgumentException if pattern is null
     */
    boolean exists(EdgePattern pattern) throws DataManagerException;

    /**
     * Finds a specific edge (for RDF-star support).
     * Use case: edge is an RDF-star triple, purpose is to get its reference node if any.
     *
     * @param edge The query edge to find in target storage
     * @return The target edge if found, otherwise the input edge
     * @throws DataManagerException     find fails
     * @throws IllegalArgumentException if edge is null
     */
    default Edge find(Edge edge) throws DataManagerException {
        if (edge == null) {
            throw new IllegalArgumentException("Edge cannot be null");
        }
        return edge;
    }
}