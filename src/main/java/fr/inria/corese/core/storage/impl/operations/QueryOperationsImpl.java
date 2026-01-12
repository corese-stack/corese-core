package fr.inria.corese.core.storage.impl.operations;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.storage.api.dataManager.support.exception.ErrorCode;
import fr.inria.corese.core.storage.api.dataManager.support.model.EdgePattern;
import fr.inria.corese.core.storage.api.dataManager.operations.QueryOperations;
import fr.inria.corese.core.storage.api.dataManager.support.exception.DataManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Implementation of query operations for CoreseGraphDataManager.
 * Converts Graph's Iterable-based API to modern Stream-based API.
 */
public class QueryOperationsImpl implements QueryOperations {

    private static final Logger logger = LoggerFactory.getLogger(QueryOperationsImpl.class);

    /** The underlying Corese graph instance to be queried. */
    private final Graph graph;

    /**
     * Constructs query operations for a specific graph.
     *
     * @param graph the Corese Graph instance to query; must not be null.
     * @throws IllegalArgumentException if the provided graph is null.
     */
    public QueryOperationsImpl(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }
        this.graph = graph;
    }

    /**
     * Executes a query based on the provided edge pattern and returns a stream of results.
     *
     * @param pattern the pattern defining filters for subject, predicate, object, and contexts.
     * @return a {@link Stream} of {@link Edge} objects matching the pattern.
     * @throws DataManagerException if the query fails at the graph level.
     * @throws IllegalArgumentException if the pattern is null.
     */
    @Override
    public Stream<Edge> query(EdgePattern pattern) throws DataManagerException {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }

        try {
            logger.debug("Querying with pattern: {}", pattern);

            // Extract pattern components for the graph iterator
            Node subject = pattern.getSubject();
            Node predicate = pattern.getPredicate();
            Node object = pattern.getObject();
            List<Node> contexts = pattern.getContexts();

            // Obtain the iterator from the Corese Graph
            Iterable<Edge> iterable = graph.iterate(subject, predicate, object, contexts);

            // Convert the Iterable to a Stream for modern API usage
            Stream<Edge> stream = StreamSupport.stream(iterable.spliterator(), false);

            logger.debug("Query executed successfully");
            return stream;

        } catch (Exception e) {
            logger.error("Failed to execute query with pattern: {}", pattern, e);
            throw new DataManagerException(
                    ErrorCode.QUERY_FAILED,
                    "Failed to execute query: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Counts the number of edges matching the provided pattern.
     * This method includes optimizations for common cases, such as counting all edges
     * or counting edges with a specific predicate, to avoid full iteration when possible.
     *
     * @param pattern the pattern defining the edges to count.
     * @return the number of matching edges.
     * @throws DataManagerException if the counting operation fails.
     */
    @Override
    public long count(EdgePattern pattern) throws DataManagerException {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }

        try {
            logger.debug("Counting with pattern: {}", pattern);

            // Optimize: if only predicate is specified, use Graph.size(predicate)
            if (!pattern.isPredicate() && pattern.isSubject() &&
                    pattern.isObject() && pattern.isContexts()) {

                int count = graph.size(pattern.getPredicate());
                logger.debug("Count (optimized by predicate): {}", count);
                return count;
            }

            // Optimization: if the pattern matches everything, use the total graph size
            if (pattern.matchesAll()) {
                int count = graph.size();
                logger.debug("Count (total graph size): {}", count);
                return count;
            }

            // General case: perform a query and count the stream elements
            try (Stream<Edge> stream = query(pattern)) {
                long count = stream.count();
                logger.debug("Count (general iteration): {}", count);
                return count;
            }

        } catch (DataManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to count with pattern: {}", pattern, e);
            throw new DataManagerException(
                    ErrorCode.QUERY_FAILED,
                    "Failed to count edges: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Checks if at least one edge matches the provided pattern.
     *
     * @param pattern the pattern defining the search criteria.
     * @return {@code true} if at least one matching edge exists, {@code false} otherwise.
     * @throws DataManagerException if the existence check fails.
     */
    @Override
    public boolean exists(EdgePattern pattern) throws DataManagerException {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }

        try {
            logger.debug("Checking existence with pattern: {}", pattern);

            // Use findFirst() to terminate iteration immediately upon finding a match
            try (Stream<Edge> stream = query(pattern)) {
                boolean exists = stream.findFirst().isPresent();
                logger.debug("Exists: {}", exists);
                return exists;
            }

        } catch (DataManagerException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to check existence with pattern: {}", pattern, e);
            throw new DataManagerException(
                    ErrorCode.QUERY_FAILED,
                    "Failed to check edge existence: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Attempts to find a specific edge instance within the graph.
     *
     * @param edge the edge template or reference to look for.
     * @return the found {@link Edge} instance from the graph, or the original edge if not found but valid.
     * @throws DataManagerException if the lookup fails.
     */
    @Override
    public Edge find(Edge edge) throws DataManagerException {
        if (edge == null) {
            throw new IllegalArgumentException("Edge cannot be null");
        }

        try {
            logger.debug("Finding edge: {}", edge);

            // Use Graph's find method which supports advanced features like RDF-star
            Edge found = graph.find(edge);

            logger.debug("Found result: {}", found);
            return found != null ? found : edge;

        } catch (Exception e) {
            logger.error("Failed to find edge: {}", edge, e);
            throw new DataManagerException(
                    ErrorCode.QUERY_FAILED,
                    "Failed to find edge: " + e.getMessage(),
                    e
            );
        }
    }
}