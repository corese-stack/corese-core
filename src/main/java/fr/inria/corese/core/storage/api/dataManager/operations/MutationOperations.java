package fr.inria.corese.core.storage.api.dataManager.operations;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.storage.api.dataManager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.dataManager.support.model.MutationResult;

import java.util.List;

/**
 * mutation operations.
 */
public interface MutationOperations {

    /**
     * Inserts a single edge.
     *
     * @param edge Edge to insert
     * @return Mutation result
     * @throws DataManagerException     if operation fails
     * @throws IllegalArgumentException if edge is null
     */
    MutationResult insertEdge(Edge edge) throws DataManagerException;

    /**
     * Inserts an edge with explicit subject, predicate, object, and contexts.
     *
     * @param subject   Subject node
     * @param predicate Predicate node
     * @param object    Object node
     * @param contexts  List of contexts (null or empty for default graph)
     * @throws DataManagerException     if operation fails
     * @throws IllegalArgumentException if subject, predicate, or object is null
     */
    MutationResult insertEdge(Node subject, Node predicate, Node object, List<Node> contexts)
            throws DataManagerException;

    /**
     * Deletes a single edge.
     *
     * @param edge Edge to delete
     * @return Mutation result
     * @throws DataManagerException     if operation fails
     * @throws IllegalArgumentException if edge is null
     */
    MutationResult deleteEdge(Edge edge) throws DataManagerException;

    /**
     * Deletes edges matching the given pattern.
     *
     * @param subject   Subject node (null for any)
     * @param predicate Predicate node (null for any)
     * @param object    Object node (null for any)
     * @param contexts  List of contexts (null or empty for all)
     * @return Mutation result (bulk result with all deleted edges)
     * @throws DataManagerException if operation fails
     */
    MutationResult deleteEdges(Node subject, Node predicate, Node object, List<Node> contexts)
            throws DataManagerException;

    /**
     * Updates an edge (delete + insert in one operation).
     *
     * @param oldEdge Edge to delete
     * @param newEdge Edge to insert
     * @return Mutation result
     * @throws DataManagerException     if operation fails
     * @throws IllegalArgumentException if oldEdge or newEdge is null
     */
    default MutationResult updateEdge(Edge oldEdge, Edge newEdge) throws DataManagerException {
        if (oldEdge == null || newEdge == null) {
            throw new IllegalArgumentException("Both oldEdge and newEdge must be non-null");
        }

        // Simple implementation: delete then insert
        // In a transactional system, this would be atomic
        MutationResult deleteResult = deleteEdge(oldEdge);
        if (deleteResult.isFailure()) {
            return deleteResult;
        }

        MutationResult insertResult = insertEdge(newEdge);
        if (insertResult.isFailure()) {
            return MutationResult.failure(
                    "Insert failed after delete: " + insertResult.getMessage()
            );
        }

        return insertResult;
    }

    /**
     * Generates a new blank node identifier.
     *
     * @return New blank node ID
     * @throws DataManagerException if generation fails
     */
    String generateBlankNode() throws DataManagerException;
}