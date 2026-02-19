package fr.inria.corese.core.next.storagemanager.impl.graph;

import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.storagemanager.api.operations.BulkOperations;
import fr.inria.corese.core.next.storagemanager.api.support.model.MutationResult;
import fr.inria.corese.core.next.storagemanager.api.support.model.StatementPattern;

import java.util.List;

/**
 * Bulk operations implementation for {@link GraphStorageManager}.
 */
public class GraphBulkOperations implements BulkOperations {

    private final GraphAdapter adapter;

    /**
     * Constructs a new GraphBulkOperations.
     *
     * @param adapter the GraphAdapter for Graph access (must not be null)
     * @throws IllegalArgumentException if adapter is null
     */
    public GraphBulkOperations(GraphAdapter adapter) {
        if (adapter == null) throw new IllegalArgumentException("GraphAdapter cannot be null");
        this.adapter = adapter;
    }

    /**
     * Inserts multiple statements in batch.
     * >
     *
     * @param statements the list of statements to insert (must not be null or empty)
     * @return a {@link MutationResult} containing success/failure counts and details
     */
    @Override
    public MutationResult insertBatch(List<Statement> statements) {
        if (statements == null || statements.isEmpty())
            throw new IllegalArgumentException("Statements cannot be null or empty");

        var builder = MutationResult.bulkBuilder().totalAttempted(statements.size());
        for (Statement stmt : statements) {
            try {
                boolean added = adapter.add(stmt);
                if (added) builder.addSuccess(stmt);
                else builder.addFailure(stmt, "Already exists");
            } catch (Exception e) {
                builder.addFailure(stmt, e.getMessage(), e);
            }
        }
        return builder.build();
    }

    /**
     * Deletes multiple statements in batch.
     *
     * @param statements the list of statements to delete (must not be null or empty)
     * @return a {@link MutationResult} containing success/failure counts and details
     */
    @Override
    public MutationResult deleteBatch(List<Statement> statements) {
        if (statements == null || statements.isEmpty())
            throw new IllegalArgumentException("Statements cannot be null or empty");

        var builder = MutationResult.bulkBuilder().totalAttempted(statements.size());
        for (Statement stmt : statements) {
            try {
                boolean removed = adapter.remove(stmt);
                if (removed) builder.addSuccess(stmt);
                else builder.addFailure(stmt, "Not found");
            } catch (Exception e) {
                builder.addFailure(stmt, e.getMessage(), e);
            }
        }
        return builder.build();
    }

    /**
     * Deletes all statements matching the given pattern.
     *
     * @param pattern the statement pattern to match (must not be null)
     * @return a {@link MutationResult} with the count of deleted statements
     */
    @Override
    public MutationResult deleteByPattern(StatementPattern pattern) {
        if (pattern == null) throw new IllegalArgumentException("Pattern cannot be null");

        var toRemove = adapter.find(
                pattern.getSubject(),
                pattern.getPredicate(),
                pattern.getObject(),
                pattern.getContexts()
        );
        int count = toRemove.size();
        toRemove.forEach(adapter::remove);

        return MutationResult.bulkBuilder()
                .totalAttempted(count)
                .successCount(count)
                .message("Deleted " + count + " by pattern")
                .build();
    }

    /**
     * Clears statements from specified contexts (named graphs).
     *
     * @param contexts the list of contexts to clear; null or empty clears entire Graph
     * @param silent   if true, suppresses errors (currently ignored)
     * @return a {@link MutationResult} with the count of deleted statements
     */
    @Override
    public MutationResult clearContexts(List<Resource> contexts, boolean silent) {
        int before = adapter.size();
        if (contexts == null || contexts.isEmpty()) {
            adapter.clear();
        } else {
            contexts.forEach(adapter::clearContext);
        }
        int deleted = before - adapter.size();

        return MutationResult.bulkBuilder()
                .totalAttempted(deleted)
                .successCount(deleted)
                .message("Cleared " + deleted + " statement(s)")
                .build();
    }
}