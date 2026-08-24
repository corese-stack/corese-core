package fr.inria.corese.core.next.storage.impl.graph;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.storage.api.operations.MutationOperations;
import fr.inria.corese.core.next.storage.api.exception.ErrorCode;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.MutationResult;

/**
 * Mutation operations implementation for {@link GraphStorageManager}.
 */
final class GraphMutationOperations implements MutationOperations {

    private final CoreseGraphStatementStore adapter;

    /**
     * Constructs a new GraphMutationOperations.
     *
     * @param adapter the CoreseGraphStatementStore for Graph access (must not be null)
     * @throws IllegalArgumentException if adapter is null
     */
    public GraphMutationOperations(CoreseGraphStatementStore adapter) {
        if (adapter == null) throw new IllegalArgumentException("CoreseGraphStatementStore cannot be null");
        this.adapter = adapter;
    }

    /**
     * Inserts a statement into the Graph.
     *
     * @param statement the statement to insert (must not be null)
     * @return a {@link MutationResult} indicating success (either "Inserted" or "Already exists")
     * @throws IllegalArgumentException if statement is null
     * @throws StorageException         if the insert operation fails
     */
    @Override
    public MutationResult insertStatement(Statement statement) throws StorageException {
        if (statement == null) throw new IllegalArgumentException("Statement cannot be null");
        try {
            boolean added = adapter.add(statement);
            return added ? MutationResult.success(statement, "Inserted")
                    : MutationResult.success(null, "Already exists");
        } catch (Exception e) {
            throw new StorageException(ErrorCode.MUTATION_FAILED, "Insert failed", e);
        }
    }

    /**
     * Inserts a statement constructed from individual components.
     *
     * @param s   the subject
     * @param p   the predicate
     * @param o   the object
     * @param ctx optional contexts
     * @return never returns (always throws)
     */
    @Override
    public MutationResult insertStatement(Resource s, IRI p, Value o, Resource... ctx) {
        throw new UnsupportedOperationException(
                "Use insertStatement(Statement) instead. " +
                        "Creating Statement from components not yet implemented."
        );
    }

    /**
     * Deletes a statement from the Graph.
     *
     * @param statement the statement to delete (must not be null)
     * @return a {@link MutationResult} indicating success or failure
     * @throws IllegalArgumentException if statement is null
     * @throws StorageException         if the delete operation fails
     */
    @Override
    public MutationResult deleteStatement(Statement statement) throws StorageException {
        if (statement == null) throw new IllegalArgumentException("Statement cannot be null");
        try {
            boolean removed = adapter.remove(statement);
            return removed ? MutationResult.success(statement, "Deleted")
                    : MutationResult.failure("Not found");
        } catch (Exception e) {
            throw new StorageException(ErrorCode.MUTATION_FAILED, "Delete failed", e);
        }
    }

    /**
     * Deletes all statements matching the given pattern.
     *
     * @param s   subject filter (null = any)
     * @param p   predicate filter (null = any)
     * @param o   object filter (null = any)
     * @param ctx context filters (null/empty = any)
     * @return a {@link MutationResult} with the count of deleted statements
     * @throws StorageException if the operation fails
     */
    @Override
    public MutationResult deleteStatements(Resource s, IRI p, Value o, Resource... ctx)
            throws StorageException {
        try {
            var toRemove = adapter.find(s, p, o, ctx);
            int count = toRemove.size();
            toRemove.forEach(adapter::remove);
            return MutationResult.bulkBuilder()
                    .totalAttempted(count)
                    .successCount(count)
                    .message("Deleted " + count + " statement(s)")
                    .build();
        } catch (Exception e) {
            throw new StorageException(ErrorCode.MUTATION_FAILED, "Delete failed", e);
        }
    }

    /**
     * Clears statements from specified contexts (named graphs).
     *
     * @param contexts the contexts to clear; null/empty clears entire Graph
     * @return a {@link MutationResult} with the count of deleted statements
     * @throws StorageException if the clear operation fails
     */
    @Override
    public MutationResult clear(Resource... contexts) throws StorageException {
        try {
            int before = adapter.size();
            if (contexts == null || contexts.length == 0) {
                adapter.clear();
            } else {
                for (Resource ctx : contexts) adapter.clearContext(ctx);
            }
            int deleted = before - adapter.size();
            return MutationResult.bulkBuilder()
                    .totalAttempted(deleted)
                    .successCount(deleted)
                    .message("Cleared " + deleted + " statement(s)")
                    .build();
        } catch (Exception e) {
            throw new StorageException(ErrorCode.MUTATION_FAILED, "Clear failed", e);
        }
    }
}
