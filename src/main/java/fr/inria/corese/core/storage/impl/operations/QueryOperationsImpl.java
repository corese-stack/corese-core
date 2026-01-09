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

    private final Graph graph;

    /**
     * Constructs query operations for a graph.
     *
     * @param graph Graph to query
     * @throws IllegalArgumentException if graph is null
     */
    public QueryOperationsImpl(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }
        this.graph = graph;
    }

    @Override
    public Stream<Edge> query(EdgePattern pattern) throws DataManagerException {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }

        try {
            logger.debug("Querying with pattern: {}", pattern);

            // Extract pattern components
            Node subject = pattern.getSubject();
            Node predicate = pattern.getPredicate();
            Node object = pattern.getObject();
            List<Node> contexts = pattern.getContexts();

            // Use Graph's iterate method
            Iterable<Edge> iterable = graph.iterate(subject, predicate, object, contexts);

            // Convert Iterable to Stream
            // parallel=false because Graph iteration may not be thread-safe
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
                logger.debug("Count (optimized): {}", count);
                return count;
            }

            // Optimize: if no constraints, use Graph.size()
            if (pattern.matchesAll()) {
                int count = graph.size();
                logger.debug("Count (total): {}", count);
                return count;
            }

            // General case: count by iterating
            try (Stream<Edge> stream = query(pattern)) {
                long count = stream.count();
                logger.debug("Count (general): {}", count);
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

    @Override
    public boolean exists(EdgePattern pattern) throws DataManagerException {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }

        try {
            logger.debug("Checking existence with pattern: {}", pattern);

            // Optimize: use findFirst() which stops after first match
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

    @Override
    public Edge find(Edge edge) throws DataManagerException {
        if (edge == null) {
            throw new IllegalArgumentException("Edge cannot be null");
        }

        try {
            logger.debug("Finding edge: {}", edge);

            // Use Graph's find method for RDF-star support
            Edge found = graph.find(edge);

            logger.debug("Found: {}", found);
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