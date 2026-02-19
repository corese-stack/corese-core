package fr.inria.corese.core.next.storagemanager.api.operations;

import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.storagemanager.api.support.exception.StorageException;
import fr.inria.corese.core.next.storagemanager.api.support.model.StatementPattern;
import fr.inria.corese.core.next.storagemanager.api.support.model.MutationResult;

import java.util.List;

/**
 * Bulk mutation operations for batch processing in Model.
 */
public interface BulkOperations {

    /**
     * Inserts multiple statements in a single batch operation.
     *
     * @param statements List of statements to insert
     * @return Bulk mutation result with statistics
     * @throws StorageException     if operation fails
     * @throws IllegalArgumentException if statements is null or empty
     */
    MutationResult insertBatch(List<Statement> statements) throws StorageException;

    /**
     * Deletes multiple statements in a single batch operation.
     * More efficient than calling deleteStatement() multiple times.
     *
     * @param statements List of statements to delete
     * @return Bulk mutation result with statistics
     * @throws StorageException     if operation fails
     * @throws IllegalArgumentException if statements is null or empty
     */
    MutationResult deleteBatch(List<Statement> statements) throws StorageException;

    /**
     * Deletes all statements matching the given pattern.
     *
     * @param pattern Pattern to match statements for deletion
     * @return Bulk mutation result with deleted statements
     * @throws StorageException     if operation fails
     * @throws IllegalArgumentException if pattern is null
     */
    MutationResult deleteByPattern(StatementPattern pattern) throws StorageException;

    /**
     * Clears (deletes all statements from) specific contexts.
     *
     * @param contexts List of contexts to clear (empty or null for all)
     * @param silent   If true, don't fail if context doesn't exist
     * @return Bulk mutation result
     * @throws StorageException if operation fails
     */
    MutationResult clearContexts(List<Resource> contexts, boolean silent) throws StorageException;

    /**
     * Clears all contexts (deletes entire model).
     *
     * @return Bulk mutation result with all deleted statements
     * @throws StorageException if operation fails
     */
    default MutationResult clearAll() throws StorageException {
        return clearContexts(null, false);
    }

    /**
     * Adds statements from source context to target context.
     * Statements remain in source context.
     *
     * @param sourceContext Source context
     * @param targetContext Target context
     * @param silent        If true, don't fail if source doesn't exist
     * @return Bulk mutation result
     * @throws StorageException     if operation fails
     * @throws IllegalArgumentException if contexts are null
     */
    default MutationResult addGraph(Resource sourceContext, Resource targetContext, boolean silent)
            throws StorageException {
        throw new UnsupportedOperationException("addGraph not implemented - Model API doesn't support this operation natively");
    }


    /**
     * Undeclares (deletes) a context and all its statements.
     *
     * @param context Context to undeclare
     * @return Bulk mutation result with deleted statements
     * @throws StorageException     if operation fails
     * @throws IllegalArgumentException if context is null
     */
    default MutationResult undeclareContext(Resource context) throws StorageException {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        return clearContexts(List.of(context), false);
    }

    /**
     * Undeclares (deletes) multiple contexts and all their statements in a single batch operation.
     * More efficient than calling undeclareContext() multiple times.
     *
     * @param contexts List of contexts to undeclare
     * @return Bulk mutation result with all deleted statements
     * @throws StorageException     if operation fails
     * @throws IllegalArgumentException if contexts is null or empty
     */
    default MutationResult undeclareContexts(List<Resource> contexts) throws StorageException {
        if (contexts == null || contexts.isEmpty()) {
            throw new IllegalArgumentException("Contexts list cannot be null or empty");
        }
        return clearContexts(contexts, false);
    }
}