package fr.inria.corese.core.storage.impl.operations;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.storage.api.dataManager.operations.BulkOperations;
import fr.inria.corese.core.storage.api.dataManager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.dataManager.support.exception.ErrorCode;
import fr.inria.corese.core.storage.api.dataManager.support.model.EdgePattern;
import fr.inria.corese.core.storage.api.dataManager.support.model.MutationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of bulk operations for CoreseGraphDataManager.
 */
public class BulkOperationsImpl implements BulkOperations {

    private static final Logger logger = LoggerFactory.getLogger(BulkOperationsImpl.class);

    private final Graph graph;

    /**
     * Constructs bulk operations for a graph.
     *
     * @param graph Graph to operate on
     * @throws IllegalArgumentException if graph is null
     */
    public BulkOperationsImpl(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }
        this.graph = graph;
    }

    @Override
    public MutationResult insertBatch(List<Edge> edges) throws DataManagerException {
        if (edges == null || edges.isEmpty()) {
            throw new IllegalArgumentException("Edges list cannot be null or empty");
        }

        logger.info("Inserting batch of {} edges", edges.size());

        MutationResult.BulkBuilder builder = MutationResult.bulkBuilder()
                .totalAttempted(edges.size())
                .message("Batch insert of " + edges.size() + " edge(s)");

        try {
            for (Edge edge : edges) {
                try {
                    Edge inserted = graph.insertEdgeWithTargetNode(edge);
                    if (inserted != null) {
                        builder.addSuccess(inserted);
                    } else {
                        builder.addFailure(edge, "Insert returned null");
                    }
                } catch (Exception e) {
                    builder.addFailure(edge, "Insert failed: " + e.getMessage(), e);
                }
            }

            MutationResult result = builder.build();
            logger.info("Batch insert completed: success={}, failure={}",
                    result.getSuccessCount(), result.getFailureCount());
            return result;

        } catch (Exception e) {
            logger.error("Batch insert failed", e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Batch insert failed: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public MutationResult deleteBatch(List<Edge> edges) throws DataManagerException {
        if (edges == null || edges.isEmpty()) {
            throw new IllegalArgumentException("Edges list cannot be null or empty");
        }

        logger.info("Deleting batch of {} edges", edges.size());

        MutationResult.BulkBuilder builder = MutationResult.bulkBuilder()
                .totalAttempted(edges.size())
                .message("Batch delete of " + edges.size() + " edge(s)");

        try {
            for (Edge edge : edges) {
                try {
                    Iterable<Edge> deleted = graph.deleteEdgeWithTargetNode(edge);

                    boolean foundAny = false;
                    if (deleted != null) {
                        for (Edge e : deleted) {
                            if (e != null) {
                                builder.addSuccess(e);
                                foundAny = true;
                            }
                        }
                    }

                    if (!foundAny) {
                        builder.addFailure(edge, "Edge not found");
                    }
                } catch (Exception e) {
                    builder.addFailure(edge, "Delete failed: " + e.getMessage(), e);
                }
            }

            MutationResult result = builder.build();
            logger.info("Batch delete completed: success={}, failure={}",
                    result.getSuccessCount(), result.getFailureCount());
            return result;

        } catch (Exception e) {
            logger.error("Batch delete failed", e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Batch delete failed: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public MutationResult deleteByPattern(EdgePattern pattern) throws DataManagerException {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }

        logger.info("Deleting by pattern: {}", pattern);

        try {
            Node subject = pattern.getSubject();
            Node predicate = pattern.getPredicate();
            Node object = pattern.getObject();
            List<Node> contexts = pattern.getContexts();

            // Use Graph's delete method
            Iterable<Edge> deleted = graph.delete(subject, predicate, object, contexts);

            // Convert to list and count
            List<Edge> deletedList = new ArrayList<>();
            if (deleted != null) {
                for (Edge e : deleted) {
                    if (e != null) {
                        deletedList.add(e);
                    }
                }
            }

            logger.info("Deleted {} edge(s) by pattern", deletedList.size());

            // Build result
            MutationResult.BulkBuilder builder = MutationResult.bulkBuilder()
                    .totalAttempted(deletedList.size())
                    .addAffectedEdges(deletedList)
                    .message("Deleted " + deletedList.size() + " edge(s) by pattern");

            for (int i = 0; i < deletedList.size(); i++) {
                builder.incrementSuccess();
            }

            return builder.build();

        } catch (Exception e) {
            logger.error("Delete by pattern failed", e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Delete by pattern failed: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public MutationResult clearContexts(List<Node> contexts, boolean silent)
            throws DataManagerException {

        if (contexts == null || contexts.isEmpty()) {
            return clearAll();
        }

        logger.info("Clearing {} context(s), silent={}", contexts.size(), silent);

        MutationResult.BulkBuilder builder = MutationResult.bulkBuilder()
                .totalAttempted(contexts.size())
                .message("Cleared " + contexts.size() + " context(s)");

        try {
            int totalDeleted = 0;

            for (Node context : contexts) {
                try {
                    // Use Graph's clear method
                    graph.clear(context.getLabel(), silent);
                    builder.incrementSuccess();
                    totalDeleted++;
                } catch (Exception e) {
                    if (!silent) {
                        builder.addFailure(null, "Failed to clear context " + context + ": " + e.getMessage(), e);
                    } else {
                        builder.incrementSuccess(); // Silent mode = consider as success
                    }
                }
            }

            logger.info("Cleared {} context(s)", totalDeleted);
            return builder.build();

        } catch (Exception e) {
            logger.error("Clear contexts failed", e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Clear contexts failed: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public MutationResult clearAll() throws DataManagerException {
        logger.warn("Clearing ALL data from graph");

        try {
            int sizeBefore = graph.size();

            // Clear all data
            graph.clear();
            graph.dropGraphNames();

            logger.info("Cleared {} edge(s) and all graph names", sizeBefore);

            return MutationResult.bulkBuilder()
                    .totalAttempted(sizeBefore)
                    .incrementSuccess()  // Consider as single successful operation
                    .message("Cleared all data (" + sizeBefore + " edge(s))")
                    .build();

        } catch (Exception e) {
            logger.error("Clear all failed", e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Clear all failed: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public MutationResult addGraph(Node sourceContext, Node targetContext, boolean silent)
            throws DataManagerException {

        if (sourceContext == null || targetContext == null) {
            throw new IllegalArgumentException("Source and target contexts cannot be null");
        }

        logger.info("Adding graph from {} to {}, silent={}", sourceContext, targetContext, silent);

        try {
            boolean success = graph.add(
                    sourceContext.getLabel(),
                    targetContext.getLabel(),
                    silent
            );

            if (success) {
                return MutationResult.bulkBuilder()
                        .totalAttempted(1)
                        .incrementSuccess()
                        .message("Added graph from " + sourceContext + " to " + targetContext)
                        .build();
            } else {
                return MutationResult.bulkBuilder()
                        .totalAttempted(1)
                        .incrementFailure()
                        .message("Failed to add graph")
                        .build();
            }

        } catch (Exception e) {
            logger.error("Add graph failed", e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Add graph failed: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public MutationResult copyGraph(Node sourceContext, Node targetContext, boolean silent)
            throws DataManagerException {

        if (sourceContext == null || targetContext == null) {
            throw new IllegalArgumentException("Source and target contexts cannot be null");
        }

        logger.info("Copying graph from {} to {}, silent={}", sourceContext, targetContext, silent);

        try {
            boolean success = graph.copy(
                    sourceContext.getLabel(),
                    targetContext.getLabel(),
                    silent
            );

            if (success) {
                return MutationResult.bulkBuilder()
                        .totalAttempted(1)
                        .incrementSuccess()
                        .message("Copied graph from " + sourceContext + " to " + targetContext)
                        .build();
            } else {
                return MutationResult.bulkBuilder()
                        .totalAttempted(1)
                        .incrementFailure()
                        .message("Failed to copy graph")
                        .build();
            }

        } catch (Exception e) {
            logger.error("Copy graph failed", e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Copy graph failed: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public MutationResult moveGraph(Node sourceContext, Node targetContext, boolean silent)
            throws DataManagerException {

        if (sourceContext == null || targetContext == null) {
            throw new IllegalArgumentException("Source and target contexts cannot be null");
        }

        logger.info("Moving graph from {} to {}, silent={}", sourceContext, targetContext, silent);

        try {
            boolean success = graph.move(
                    sourceContext.getLabel(),
                    targetContext.getLabel(),
                    silent
            );

            if (success) {
                return MutationResult.bulkBuilder()
                        .totalAttempted(1)
                        .incrementSuccess()
                        .message("Moved graph from " + sourceContext + " to " + targetContext)
                        .build();
            } else {
                return MutationResult.bulkBuilder()
                        .totalAttempted(1)
                        .incrementFailure()
                        .message("Failed to move graph")
                        .build();
            }

        } catch (Exception e) {
            logger.error("Move graph failed", e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Move graph failed: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public MutationResult declareContext(Node context) throws DataManagerException {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        logger.debug("Declaring context: {}", context);

        try {
            graph.addGraphNode(context);

            return MutationResult.success(null, "Context declared: " + context);

        } catch (Exception e) {
            logger.error("Declare context failed", e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Declare context failed: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public MutationResult undeclareContext(Node context) throws DataManagerException {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        logger.info("Undeclaring context: {}", context);

        try {
            // Clear the context (deletes all edges)
            return clearContexts(List.of(context), false);

        } catch (Exception e) {
            logger.error("Undeclare context failed", e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Undeclare context failed: " + e.getMessage(),
                    e
            );
        }
    }
}