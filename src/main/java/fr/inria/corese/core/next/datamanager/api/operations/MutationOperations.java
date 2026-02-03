package fr.inria.corese.core.next.datamanager.api.operations;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.datamanager.api.support.exception.DataManagerException;
import fr.inria.corese.core.next.datamanager.api.support.model.MutationResult;

/**
 * Mutation operations for Model.
 * Provides insert, delete, and update operations for statements.
 */
public interface MutationOperations {

    /**
     * Inserts a single statement.
     *
     * @param statement Statement to insert
     * @return Mutation result
     * @throws DataManagerException     if operation fails
     * @throws IllegalArgumentException if statement is null
     */
    MutationResult insertStatement(Statement statement) throws DataManagerException;

    /**
     * Inserts a statement with explicit subject, predicate, object, and contexts.
     *
     * @param subject   Subject resource
     * @param predicate Predicate IRI
     * @param object    Object value
     * @param contexts  Array of contexts (empty for default graph)
     * @return Mutation result
     * @throws DataManagerException     if operation fails
     * @throws IllegalArgumentException if subject, predicate, or object is null
     */
    MutationResult insertStatement(Resource subject, IRI predicate, Value object, Resource... contexts)
            throws DataManagerException;

    /**
     * Deletes a single statement.
     *
     * @param statement Statement to delete
     * @return Mutation result
     * @throws DataManagerException     if operation fails
     * @throws IllegalArgumentException if statement is null
     */
    MutationResult deleteStatement(Statement statement) throws DataManagerException;

    /**
     * Deletes statements matching the given pattern.
     *
     * @param subject   Subject resource (null for any)
     * @param predicate Predicate IRI (null for any)
     * @param object    Object value (null for any)
     * @param contexts  Array of contexts (null or empty for all)
     * @return Mutation result (bulk result with all deleted statements)
     * @throws DataManagerException if operation fails
     */
    MutationResult deleteStatements(Resource subject, IRI predicate, Value object, Resource... contexts)
            throws DataManagerException;

    /**
     * Updates a statement (delete + insert in one operation).
     *
     * @param oldStatement Statement to delete
     * @param newStatement Statement to insert
     * @return Mutation result
     * @throws DataManagerException     if operation fails
     * @throws IllegalArgumentException if oldStatement or newStatement is null
     */
    default MutationResult updateStatement(Statement oldStatement, Statement newStatement)
            throws DataManagerException {
        if (oldStatement == null || newStatement == null) {
            throw new IllegalArgumentException("Both oldStatement and newStatement must be non-null");
        }

        MutationResult deleteResult = deleteStatement(oldStatement);
        if (deleteResult.isFailure()) {
            return deleteResult;
        }

        MutationResult insertResult = insertStatement(newStatement);
        if (insertResult.isFailure()) {
            return MutationResult.failure(
                    "Insert failed after delete: " + insertResult.getMessage()
            );
        }

        return insertResult;
    }

    /**
     * Clears statements from the specified contexts.
     *
     * @param contexts Array of contexts to clear (empty for all)
     * @return Mutation result with count of deleted statements
     * @throws DataManagerException if operation fails
     */
    MutationResult clear(Resource... contexts) throws DataManagerException;
}