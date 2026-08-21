package fr.inria.corese.core.next.storage.api.operations;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.MutationResult;

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
     * @throws StorageException     if operation fails
     * @throws IllegalArgumentException if statement is null
     */
    MutationResult insertStatement(Statement statement) throws StorageException;

    /**
     * Inserts a statement with explicit subject, predicate, object, and contexts.
     *
     * @param subject   Subject resource
     * @param predicate Predicate IRI
     * @param object    Object value
     * @param contexts  Array of contexts (empty for default graph)
     * @return Mutation result
     * @throws StorageException     if operation fails
     * @throws IllegalArgumentException if subject, predicate, or object is null
     */
    MutationResult insertStatement(Resource subject, IRI predicate, Value object, Resource... contexts)
            throws StorageException;

    /**
     * Deletes a single statement.
     *
     * @param statement Statement to delete
     * @return Mutation result
     * @throws StorageException     if operation fails
     * @throws IllegalArgumentException if statement is null
     */
    MutationResult deleteStatement(Statement statement) throws StorageException;

    /**
     * Deletes statements matching the given pattern.
     *
     * @param subject   Subject resource (null for any)
     * @param predicate Predicate IRI (null for any)
     * @param object    Object value (null for any)
     * @param contexts  Array of contexts (null or empty for all)
     * @return Mutation result (bulk result with all deleted statements)
     * @throws StorageException if operation fails
     */
    MutationResult deleteStatements(Resource subject, IRI predicate, Value object, Resource... contexts)
            throws StorageException;

    /**
     * Clears statements from the specified contexts.
     *
     * @param contexts Array of contexts to clear (empty for all)
     * @return Mutation result with count of deleted statements
     * @throws StorageException if operation fails
     */
    MutationResult clear(Resource... contexts) throws StorageException;
}