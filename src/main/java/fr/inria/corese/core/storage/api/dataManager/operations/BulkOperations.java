package fr.inria.corese.core.storage.api.dataManager.operations;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.storage.api.dataManager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.dataManager.support.model.EdgePattern;
import fr.inria.corese.core.storage.api.dataManager.support.model.MutationResult;

import java.util.List;

/**
 * Bulk mutation operations for batch processing.
 */
public interface BulkOperations {

    /**
     * Inserts multiple edges in a single batch operation.
     *
     * @param edges List of edges to insert
     * @return Bulk mutation result with statistics
     * @throws DataManagerException     if operation fails
     * @throws IllegalArgumentException if edges is null or empty
     */
    MutationResult insertBatch(List<Edge> edges) throws DataManagerException;

    /**
     * Deletes multiple edges in a single batch operation.
     * More efficient than calling deleteEdge() multiple times.
     *
     * @param edges List of edges to delete
     * @return Bulk mutation result with statistics
     * @throws DataManagerException     if operation fails
     * @throws IllegalArgumentException if edges is null or empty
     */
    MutationResult deleteBatch(List<Edge> edges) throws DataManagerException;

    /**
     * Deletes all edges matching the given pattern.
     *
     * @param pattern Pattern to match edges for deletion
     * @return Bulk mutation result with deleted edges
     * @throws DataManagerException     if operation fails
     * @throws IllegalArgumentException if pattern is null
     */
    MutationResult deleteByPattern(EdgePattern pattern) throws DataManagerException;

    /**
     * Clears (deletes all edges from) specific contexts.
     *
     * @param contexts List of contexts to clear (empty or null for all)
     * @param silent   If true, don't fail if context doesn't exist
     * @return Bulk mutation result
     * @throws DataManagerException if operation fails
     */
    MutationResult clearContexts(List<Node> contexts, boolean silent) throws DataManagerException;

    /**
     * Clears all contexts (deletes entire graph).
     *
     * @return Bulk mutation result with all deleted edges
     * @throws DataManagerException if operation fails
     */
    MutationResult clearAll() throws DataManagerException;

    /**
     * Adds edges from source context to target context.
     * Edges remain in source context.
     *
     * @param sourceContext Source context
     * @param targetContext Target context
     * @param silent        If true, don't fail if source doesn't exist
     * @return Bulk mutation result
     * @throws DataManagerException     if operation fails
     * @throws IllegalArgumentException if contexts are null
     */
    MutationResult addGraph(Node sourceContext, Node targetContext, boolean silent)
            throws DataManagerException;

    /**
     * Copies edges from source context to target context.
     * Replaces all edges in target context.
     *
     * @param sourceContext Source context
     * @param targetContext Target context
     * @param silent        If true, don't fail if source doesn't exist
     * @return Bulk mutation result
     * @throws DataManagerException     if operation fails
     * @throws IllegalArgumentException if contexts are null
     */
    MutationResult copyGraph(Node sourceContext, Node targetContext, boolean silent)
            throws DataManagerException;

    /**
     * Moves edges from source context to target context.
     * Removes edges from source context.
     *
     * @param sourceContext Source context
     * @param targetContext Target context
     * @param silent        If true, don't fail if source doesn't exist
     * @return Bulk mutation result
     * @throws DataManagerException     if operation fails
     * @throws IllegalArgumentException if contexts are null
     */
    MutationResult moveGraph(Node sourceContext, Node targetContext, boolean silent)
            throws DataManagerException;

    /**
     * Declares (creates) a context without adding edges.
     * Useful for pre-creating named graphs.
     *
     * @param context Context to declare
     * @return Mutation result
     * @throws DataManagerException     if operation fails
     * @throws IllegalArgumentException if context is null
     */
    MutationResult declareContext(Node context) throws DataManagerException;

    /**
     * Undeclares (deletes) a context and all its edges.
     *
     * @param context Context to undeclare
     * @return Bulk mutation result with deleted edges
     * @throws DataManagerException     if operation fails
     * @throws IllegalArgumentException if context is null
     */
    MutationResult undeclareContext(Node context) throws DataManagerException;

}