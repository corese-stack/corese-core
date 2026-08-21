package fr.inria.corese.core.next.storage.api.operations;

import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import fr.inria.corese.core.next.storage.api.model.MutationResult;

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

}