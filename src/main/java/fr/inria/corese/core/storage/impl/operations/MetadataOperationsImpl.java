package fr.inria.corese.core.storage.impl.operations;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.storage.api.dataManager.operations.MetadataOperations;
import fr.inria.corese.core.storage.api.dataManager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.dataManager.support.exception.ErrorCode;
import fr.inria.corese.core.storage.api.dataManager.support.model.GraphStatistics;
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

    private final Graph graph;

    /**
     * Constructs metadata operations for a graph.
     *
     * @param graph Graph to query
     * @throws IllegalArgumentException if graph is null
     */
    public MetadataOperationsImpl(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }
        this.graph = graph;
    }

    @Override
    public Set<Node> getPredicates(Node context) throws DataManagerException {
        try {
            logger.debug("Getting predicates for context: {}", context);

            // Graph.getSortedProperties() returns all predicates
            // Note: context parameter is not used by Graph implementation
            Iterable<Node> iterable = graph.getSortedProperties();

            // Convert to Set
            Set<Node> predicates = iterableToSet(iterable);

            logger.debug("Found {} predicates", predicates.size());
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

    @Override
    public Set<Node> getNodes(Node context) throws DataManagerException {
        try {
            logger.debug("Getting nodes for context: {}", context);

            Iterable<Node> iterable;
            if (context == null) {
                // All nodes
                iterable = graph.getNodeGraphIterator();
            } else {
                // Nodes in specific context
                Node graphNode = graph.getNode(context);
                iterable = graph.getNodeGraphIterator(graphNode);
            }

            // Convert to Set
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

    @Override
    public Set<Node> getContexts() throws DataManagerException {
        try {
            logger.debug("Getting all contexts");

            // Graph.getGraphNodes() returns all named graphs
            Iterable<Node> iterable = graph.getGraphNodes(new ArrayList<>(0));

            // Convert to Set
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

    @Override
    public GraphStatistics getStatistics() throws DataManagerException {
        try {
            logger.debug("Collecting graph statistics");

            // Collect statistics
            long edgeCount = graph.size();
            long nodeCount = getNodes(null).size();
            long predicateCount = getPredicates(null).size();
            long contextCount = getContexts().size();

            // Build GraphStatistics with Builder pattern
            GraphStatistics stats = GraphStatistics.builder()
                    .edgeCount(edgeCount)
                    .nodeCount(nodeCount)
                    .predicateCount(predicateCount)
                    .contextCount(contextCount)
                    .build();

            logger.debug("Statistics: {}", stats);
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
     * Converts an Iterable to a Set.
     * Helper method to convert Graph's Iterable results to Sets.
     *
     * @param iterable Iterable to convert
     * @return Set containing all elements
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