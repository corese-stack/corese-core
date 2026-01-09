package fr.inria.corese.core.storage.impl.operations;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.storage.api.dataManager.operations.MutationOperations;
import fr.inria.corese.core.storage.api.dataManager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.dataManager.support.exception.ErrorCode;
import fr.inria.corese.core.storage.api.dataManager.support.model.MutationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of mutation operations for CoreseGraphDataManager.
 */
public class MutationOperationsImpl implements MutationOperations {

    private static final Logger logger = LoggerFactory.getLogger(MutationOperationsImpl.class);

    private final Graph graph;

    /**
     * Constructs mutation operations for a graph.
     *
     * @param graph Graph to mutate
     * @throws IllegalArgumentException if graph is null
     */
    public MutationOperationsImpl(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }
        this.graph = graph;
    }

    @Override
    public MutationResult insertEdge(Edge edge) throws DataManagerException {
        if (edge == null) {
            throw new IllegalArgumentException("Edge cannot be null");
        }

        try {
            logger.debug("Inserting edge: {}", edge);

            // Use Graph's insertEdgeWithTargetNode
            Edge inserted = graph.insertEdgeWithTargetNode(edge);

            if (inserted != null) {
                logger.debug("Edge inserted successfully");
                return MutationResult.success(inserted, "Edge inserted");
            } else {
                logger.warn("Edge insertion returned null");
                return MutationResult.failure("Edge insertion failed");
            }

        } catch (Exception e) {
            logger.error("Failed to insert edge: {}", edge, e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Failed to insert edge: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public MutationResult insertEdge(Node subject, Node predicate, Node object, List<Node> contexts)
            throws DataManagerException {

        if (subject == null || predicate == null || object == null) {
            throw new IllegalArgumentException("Subject, predicate, and object cannot be null");
        }

        try {
            logger.debug("Inserting edge: ({}, {}, {}) in contexts: {}",
                    subject, predicate, object, contexts);

            // Track size before insert
            int sizeBefore = graph.size();

            // Use Graph's insert method
            Iterable<Edge> inserted = graph.insert(subject, predicate, object, contexts);

            // Convert Iterable to List
            List<Edge> insertedList = new ArrayList<>();
            if (inserted != null) {
                for (Edge e : inserted) {
                    if (e != null) {
                        insertedList.add(e);
                    }
                }
            }

            // Check if insertion actually happened
            int sizeAfter = graph.size();
            boolean insertionOccurred = (sizeAfter > sizeBefore) || !insertedList.isEmpty();

            if (insertionOccurred) {
                logger.debug("Inserted edge successfully");

                // If we got edges back, use them
                if (!insertedList.isEmpty()) {
                    if (insertedList.size() == 1) {
                        return MutationResult.success(insertedList.getFirst(), "Edge inserted");
                    }

                    // Multiple edges (multiple contexts)
                    MutationResult.BulkBuilder builder = MutationResult.bulkBuilder()
                            .totalAttempted(insertedList.size())
                            .addAffectedEdges(insertedList)
                            .message("Inserted " + insertedList.size() + " edge(s)");

                    for (int i = 0; i < insertedList.size(); i++) {
                        builder.incrementSuccess();
                    }

                    return builder.build();
                } else {
                    // Insertion happened but no edges returned - create edge reference
                    Edge edge = graph.create(
                            contexts != null && !contexts.isEmpty() ? contexts.getFirst() : graph.getDefaultGraphNode(),
                            subject, predicate, object
                    );
                    return MutationResult.success(edge, "Edge inserted (verified by size change)");
                }
            } else {
                logger.warn("Edge insertion did not change graph size (possible duplicate)");
                // May be a duplicate - still return success with note
                Edge edge = graph.create(
                        contexts != null && !contexts.isEmpty() ? contexts.getFirst() : graph.getDefaultGraphNode(),
                        subject, predicate, object
                );
                return MutationResult.success(edge, "Edge may already exist");
            }

        } catch (Exception e) {
            logger.error("Failed to insert edge: ({}, {}, {})", subject, predicate, object, e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Failed to insert edge: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public MutationResult deleteEdge(Edge edge) throws DataManagerException {
        if (edge == null) {
            throw new IllegalArgumentException("Edge cannot be null");
        }

        try {
            logger.debug("Deleting edge: {}", edge);

            // Use Graph's deleteEdgeWithTargetNode
            Iterable<Edge> deleted = graph.deleteEdgeWithTargetNode(edge);

            // Convert to list
            List<Edge> deletedList = new ArrayList<>();
            if (deleted != null) {
                for (Edge e : deleted) {
                    if (e != null) {
                        deletedList.add(e);
                    }
                }
            }

            if (!deletedList.isEmpty()) {
                // Return first deleted edge (typically only one)
                return MutationResult.success(deletedList.getFirst(), "Edge deleted");
            } else {
                logger.warn("Edge deletion returned empty (edge may not exist)");
                return MutationResult.failure("Edge not found or deletion failed");
            }

        } catch (Exception e) {
            logger.error("Failed to delete edge: {}", edge, e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Failed to delete edge: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public MutationResult deleteEdges(Node subject, Node predicate, Node object, List<Node> contexts)
            throws DataManagerException {

        try {
            logger.debug("Deleting edges: ({}, {}, {}) in contexts: {}",
                    subject, predicate, object, contexts);

            // Use graph.iterate() to find matching edges (supports null wildcards)
            Iterable<Edge> matchingEdges = graph.iterate(subject, predicate, object, contexts);

            // Delete each matching edge
            List<Edge> deletedList = new ArrayList<>();
            for (Edge edge : matchingEdges) {
                if (edge != null) {
                    List<Edge> deleted = graph.delete(edge);
                    if (deleted != null) {
                        for (Edge e : deleted) {
                            if (e != null) {
                                deletedList.add(e);
                            }
                        }
                    }
                }
            }

            logger.debug("Deleted {} edge(s)", deletedList.size());

            // Build bulk result
            MutationResult.BulkBuilder builder = MutationResult.bulkBuilder()
                    .totalAttempted(deletedList.size())
                    .addAffectedEdges(deletedList)
                    .message("Deleted " + deletedList.size() + " edge(s)");

            for (int i = 0; i < deletedList.size(); i++) {
                builder.incrementSuccess();
            }

            return builder.build();

        } catch (Exception e) {
            logger.error("Failed to delete edges: ({}, {}, {})", subject, predicate, object, e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Failed to delete edges: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public String generateBlankNode() throws DataManagerException {
        try {
            String blankId = graph.newBlankID();
            logger.debug("Generated blank node: {}", blankId);
            return blankId;
        } catch (Exception e) {
            logger.error("Failed to generate blank node", e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Failed to generate blank node: " + e.getMessage(),
                    e
            );
        }
    }
}