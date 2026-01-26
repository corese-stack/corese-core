package fr.inria.corese.core.storage.impl.operations;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.storage.api.datamanager.operations.MutationOperations;
import fr.inria.corese.core.storage.api.datamanager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.datamanager.support.exception.ErrorCode;
import fr.inria.corese.core.storage.api.datamanager.support.model.MutationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of mutation operations for CoreseGraphDataManager.
 */
public class MutationOperationsImpl implements MutationOperations {

    private static final Logger logger = LoggerFactory.getLogger(MutationOperationsImpl.class);

    /** The underlying Corese graph instance to be mutated. */
    private final Graph graph;

    /**
     * Constructs mutation operations for a specific graph.
     *
     * @param graph the Corese Graph to mutate; must not be null.
     * @throws IllegalArgumentException if the provided graph is null.
     */
    public MutationOperationsImpl(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }
        this.graph = graph;
    }

    /**
     * Inserts a single edge into the graph.
     *
     * @param edge the edge object to insert.
     * @return a {@link MutationResult} indicating success or failure.
     * @throws DataManagerException if the insertion fails at the storage level.
     */
    @Override
    public MutationResult insertEdge(Edge edge) throws DataManagerException {
        if (edge == null) {
            throw new IllegalArgumentException("Edge cannot be null");
        }

        try {
            logger.debug("Inserting edge: {}", edge);

            // Use Graph's specific method for inserting an edge while ensuring target nodes exist
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

    /**
     * Inserts an edge defined by its individual components into the specified contexts.
     *
     * @param subject the subject node.
     * @param predicate the predicate node.
     * @param object the object node.
     * @param contexts the list of context (named graph) nodes where the edge should be stored.
     * @return a {@link MutationResult} summarizing the insertion.
     * @throws DataManagerException if the insertion fails.
     */
    @Override
    public MutationResult insertEdge(Node subject, Node predicate, Node object, List<Node> contexts)
            throws DataManagerException {

        if (subject == null || predicate == null || object == null) {
            throw new IllegalArgumentException("Subject, predicate, and object cannot be null");
        }

        try {
            logger.debug("Inserting edge: ({}, {}, {}) in contexts: {}",
                    subject, predicate, object, contexts);

            // Capture graph size to verify insertion if the result set is ambiguous
            int sizeBefore = graph.size();

            // Perform the insertion in the Corese graph
            Iterable<Edge> inserted = graph.insert(subject, predicate, object, contexts);

            // Process returned edges
            List<Edge> insertedList = new ArrayList<>();
            if (inserted != null) {
                for (Edge e : inserted) {
                    if (e != null) {
                        insertedList.add(e);
                    }
                }
            }

            int sizeAfter = graph.size();
            boolean insertionOccurred = (sizeAfter > sizeBefore) || !insertedList.isEmpty();

            if (insertionOccurred) {
                logger.debug("Inserted edge successfully");

                if (!insertedList.isEmpty()) {
                    if (insertedList.size() == 1) {
                        return MutationResult.success(insertedList.getFirst(), "Edge inserted");
                    }

                    // Handle multi-context insertion results
                    MutationResult.BulkBuilder builder = MutationResult.bulkBuilder()
                            .totalAttempted(insertedList.size())
                            .addAffectedEdges(insertedList)
                            .message("Inserted " + insertedList.size() + " edge(s)");

                    for (int i = 0; i < insertedList.size(); i++) {
                        builder.incrementSuccess();
                    }

                    return builder.build();
                } else {
                    // Fallback edge creation for result reporting if the iterator was empty but size changed
                    Edge edge = graph.create(
                            contexts != null && !contexts.isEmpty() ? contexts.getFirst() : graph.getDefaultGraphNode(),
                            subject, predicate, object
                    );
                    return MutationResult.success(edge, "Edge inserted (verified by size change)");
                }
            } else {
                logger.warn("Edge insertion did not change graph size (possible duplicate)");
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

    /**
     * Deletes a specific edge object from the graph.
     *
     * @param edge the edge to delete.
     * @return a {@link MutationResult} indicating if the edge was found and deleted.
     * @throws DataManagerException if the deletion fails.
     */
    @Override
    public MutationResult deleteEdge(Edge edge) throws DataManagerException {
        if (edge == null) {
            throw new IllegalArgumentException("Edge cannot be null");
        }

        try {
            logger.debug("Deleting edge: {}", edge);

            // Corese graph returns an Iterable of deleted edges
            Iterable<Edge> deleted = graph.deleteEdgeWithTargetNode(edge);

            List<Edge> deletedList = new ArrayList<>();
            if (deleted != null) {
                for (Edge e : deleted) {
                    if (e != null) {
                        deletedList.add(e);
                    }
                }
            }

            if (!deletedList.isEmpty()) {
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

    /**
     * Deletes edges matching the specified subject, predicate, and object in the given contexts.
     * Supports wildcards (null values).
     *
     * @param subject the subject node (or null for wildcard).
     * @param predicate the predicate node (or null for wildcard).
     * @param object the object node (or null for wildcard).
     * @param contexts the list of contexts to search in.
     * @return a {@link MutationResult} summarizing all deleted edges.
     * @throws DataManagerException if the deletion process fails.
     */
    @Override
    public MutationResult deleteEdges(Node subject, Node predicate, Node object, List<Node> contexts)
            throws DataManagerException {

        try {
            logger.debug("Deleting edges: ({}, {}, {}) in contexts: {}",
                    subject, predicate, object, contexts);

            // Iterate over matching edges based on pattern
            Iterable<Edge> matchingEdges = graph.iterate(subject, predicate, object, contexts);

            List<Edge> deletedList = new ArrayList<>();
            for (Edge edge : matchingEdges) {
                if (edge != null) {
                    // Delete the specific edge instance
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

    /**
     * Generates a unique blank node identifier within the graph context.
     *
     * @return a new blank node ID string.
     * @throws DataManagerException if the ID generation fails.
     */
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