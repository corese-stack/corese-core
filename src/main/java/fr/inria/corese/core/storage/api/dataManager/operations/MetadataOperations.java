package fr.inria.corese.core.storage.api.dataManager.operations;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.storage.api.dataManager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.dataManager.support.model.GraphStatistics;
import java.util.Set;

/**
 * Metadata operations for the graph.
 * Provides access to graph structure information (predicates, nodes, contexts, statistics).
 *
 */
public interface MetadataOperations {

    /**
     * Returns all predicates in the specified context.
     *
     * @param context Context to query (null for all contexts)
     * @return Set of predicates (unmodifiable)
     * @throws DataManagerException if query fails
     */
    Set<Node> getPredicates(Node context) throws DataManagerException;

    /**
     * Returns all predicates in all contexts.
     * Convenience method equivalent to getPredicates(null).
     *
     * @return Set of all predicates (unmodifiable)
     * @throws DataManagerException if query fails
     */
    default Set<Node> getAllPredicates() throws DataManagerException {
        return getPredicates(null);
    }

    /**
     * Returns all nodes (subjects and objects) in the specified context.
     *
     * @param context Context to query (null for all contexts)
     * @return Set of nodes (unmodifiable)
     * @throws DataManagerException if query fails
     */
    Set<Node> getNodes(Node context) throws DataManagerException;

    /**
     * Returns all nodes in all contexts.
     * Convenience method equivalent to getNodes(null).
     *
     * @return Set of all nodes (unmodifiable)
     * @throws DataManagerException if query fails
     */
    default Set<Node> getAllNodes() throws DataManagerException {
        return getNodes(null);
    }

    /**
     * Returns all contexts (named graphs).
     *
     * @return Set of context nodes (unmodifiable)
     * @throws DataManagerException if query fails
     */
    Set<Node> getContexts() throws DataManagerException;

    /**
     * Returns statistics about the graph content and structure.
     *
     * @return Graph statistics
     * @throws DataManagerException if statistics cannot be computed
     */
    GraphStatistics getStatistics() throws DataManagerException;
}