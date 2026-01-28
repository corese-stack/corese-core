package fr.inria.corese.core.storage.impl.operations;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.storage.api.datamanager.operations.MetadataOperations;
import fr.inria.corese.core.storage.api.datamanager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.datamanager.support.exception.ErrorCode;
import fr.inria.corese.core.storage.api.datamanager.support.model.GraphStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of metadata operations for CoreseGraphDataManager.
 * Converts Graph's Iterable-based API to Set-based API.
 */
public class MetadataOperationsImpl implements MetadataOperations {

    private static final Logger logger = LoggerFactory.getLogger(MetadataOperationsImpl.class);

    /**
     * The underlying Corese graph instance.
     */
    private final Graph graph;

    /**
     * Constructs a new metadata operations handler for the specified graph.
     *
     * @param graph the Corese Graph to query; must not be null.
     * @throws IllegalArgumentException if the provided graph is null.
     */
    public MetadataOperationsImpl(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }
        this.graph = graph;
    }

    /**
     * Retrieves all unique predicates used within the specified context.
     *
     * @param context the context (named graph) node. Note: Standard Corese implementation currently returns all predicates.
     * @return an unmodifiable Set of predicate nodes.
     * @throws DataManagerException if the metadata retrieval fails.
     */
    @Override
    public Set<Node> getPredicates(Node context) throws DataManagerException {
        try {
            logger.debug("Getting predicates for context: {}", context);

            // Graph.getSortedProperties() returns all predicates in the graph
            Iterable<Node> iterable = graph.getSortedProperties();

            // Convert to Set to ensure uniqueness and provide standard API access
            Set<Node> predicates = iterableToSet(iterable);

            logger.debug("Found {} unique predicates", predicates.size());
            return Collections.unmodifiableSet(predicates);

        } catch (Exception e) {
            logger.error("Failed to get predicates for context: {}", context, e);
            throw new DataManagerException(
                    ErrorCode.QUERY_FAILED,
                    "Failed to get predicates: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Retrieves all unique nodes (subjects or objects) within the specified context.
     *
     * @param context the context node to filter by; if null, retrieves nodes from all contexts.
     * @return an unmodifiable Set of nodes.
     * @throws DataManagerException if the node retrieval fails.
     */
    @Override
    public Set<Node> getNodes(Node context) throws DataManagerException {
        try {
            logger.debug("Getting nodes for context: {}", context);

            Iterable<Node> iterable;
            if (context == null) {
                // Retrieves an iterator over all nodes in the entire graph
                iterable = graph.getNodeGraphIterator();
            } else {
                // Retrieves an iterator over nodes specific to the named graph
                Node graphNode = graph.getNode(context);
                iterable = graph.getNodeGraphIterator(graphNode);
            }

            Set<Node> nodes = iterableToSet(iterable);

            logger.debug("Found {} nodes", nodes.size());
            return Collections.unmodifiableSet(nodes);

        } catch (Exception e) {
            logger.error("Failed to get nodes for context: {}", context, e);
            throw new DataManagerException(
                    ErrorCode.QUERY_FAILED,
                    "Failed to get nodes: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Retrieves all named graph identifiers (contexts) currently defined in the system.
     *
     * @return an unmodifiable Set of context nodes.
     * @throws DataManagerException if the context retrieval fails.
     */
    @Override
    public Set<Node> getContexts() throws DataManagerException {
        try {
            logger.debug("Getting all contexts");

            // Graph.getGraphNodes() returns nodes representing named graphs
            Iterable<Node> iterable = graph.getGraphNodes(new ArrayList<>(0));

            Set<Node> contexts = iterableToSet(iterable);

            logger.debug("Found {} contexts", contexts.size());
            return Collections.unmodifiableSet(contexts);

        } catch (Exception e) {
            logger.error("Failed to get contexts", e);
            throw new DataManagerException(
                    ErrorCode.QUERY_FAILED,
                    "Failed to get contexts: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Collects and returns general statistics about the graph size and density.
     *
     * @return a {@link GraphStatistics} object containing counts for edges, nodes, predicates, and contexts.
     * @throws DataManagerException if statistics collection fails.
     */
    @Override
    public GraphStatistics getStatistics() throws DataManagerException {
        try {
            logger.debug("Collecting graph statistics");

            // Aggregate metrics from current graph state
            long edgeCount = graph.size();
            long nodeCount = getNodes(null).size();
            long predicateCount = getPredicates(null).size();
            long contextCount = getContexts().size();

            // Create GraphStatistics using constructor with parameters
            GraphStatistics stats = new GraphStatistics(
                    edgeCount,
                    nodeCount,
                    predicateCount,
                    contextCount,
                    0, 0,0
            );

            logger.debug("Statistics collected: {}", stats);
            return stats;

        } catch (Exception e) {
            logger.error("Failed to collect statistics", e);
            throw new DataManagerException(
                    ErrorCode.QUERY_FAILED,
                    "Failed to collect statistics: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Helper method to convert a Corese {@link Iterable} of nodes into a {@link HashSet}.
     * Ensures that null nodes are ignored and duplicates are removed.
     *
     * @param iterable the iterable to convert.
     * @return a Set containing the extracted nodes.
     */
    private Set<Node> iterableToSet(Iterable<Node> iterable) {
        Set<Node> set = new HashSet<>();
        if (iterable != null) {
            for (Node node : iterable) {
                if (node != null) {
                    set.add(node);
                }
            }
        }
        return set;
    }
}