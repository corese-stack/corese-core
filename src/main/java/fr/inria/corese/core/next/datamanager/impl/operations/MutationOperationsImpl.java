package fr.inria.corese.core.next.datamanager.impl.operations;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.datamanager.api.operations.MutationOperations;
import fr.inria.corese.core.next.datamanager.api.support.exception.DataManagerException;
import fr.inria.corese.core.next.datamanager.api.support.exception.ErrorCode;
import fr.inria.corese.core.next.datamanager.api.support.model.MutationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of mutation operations for CoreseModelDataManager.
 */
public class MutationOperationsImpl implements MutationOperations {

    private static final Logger logger = LoggerFactory.getLogger(MutationOperationsImpl.class);

    /** The underlying Corese model instance to be mutated. */
    private final Model model;

    /**
     * Constructs mutation operations for a specific model.
     *
     * @param model the Corese Model to mutate; must not be null.
     * @throws IllegalArgumentException if the provided model is null.
     */
    public MutationOperationsImpl(Model model) {
        if (model == null) {
            throw new IllegalArgumentException("Model cannot be null");
        }
        this.model = model;
    }

    /**
     * Inserts a single statement into the model.
     *
     * @param statement the statement object to insert.
     * @return a {@link MutationResult} indicating success or failure.
     * @throws DataManagerException if the insertion fails at the storage level.
     */
    @Override
    public MutationResult insertStatement(Statement statement) throws DataManagerException {
        if (statement == null) {
            throw new IllegalArgumentException("Statement cannot be null");
        }

        try {
            logger.debug("Inserting statement: {}", statement);

            boolean added = model.add(statement);

            if (added) {
                logger.debug("Statement inserted successfully");
                return MutationResult.success(statement, "Statement inserted");
            } else {
                logger.warn("Statement was not inserted (may already exist)");
                return MutationResult.success(statement, "Statement already exists");
            }

        } catch (Exception e) {
            logger.error("Failed to insert statement: {}", statement, e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Failed to insert statement: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Inserts a statement defined by its individual components into the specified contexts.
     *
     * @param subject the subject resource.
     * @param predicate the predicate IRI.
     * @param object the object value.
     * @param contexts the array of context (named graph) resources where the statement should be stored.
     * @return a {@link MutationResult} summarizing the insertion.
     * @throws DataManagerException if the insertion fails.
     */
    @Override
    public MutationResult insertStatement(Resource subject, IRI predicate, Value object, Resource... contexts)
            throws DataManagerException {

        if (subject == null || predicate == null || object == null) {
            throw new IllegalArgumentException("Subject, predicate, and object cannot be null");
        }

        try {
            logger.debug("Inserting statement: ({}, {}, {}) in contexts: {}",
                    subject, predicate, object, contexts);

            boolean added = model.add(subject, predicate, object, contexts);

            if (added) {
                // For now, we return a simple success result
                // In a real implementation, you might want to create a Statement object
                return MutationResult.success(null, "Statement inserted");
            } else {
                return MutationResult.success(null, "Statement already exists");
            }

        } catch (Exception e) {
            logger.error("Failed to insert statement: ({}, {}, {})", subject, predicate, object, e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Failed to insert statement: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Deletes a specific statement object from the model.
     *
     * @param statement the statement to delete.
     * @return a {@link MutationResult} indicating if the statement was found and deleted.
     * @throws DataManagerException if the deletion fails.
     */
    @Override
    public MutationResult deleteStatement(Statement statement) throws DataManagerException {
        if (statement == null) {
            throw new IllegalArgumentException("Statement cannot be null");
        }

        try {
            logger.debug("Deleting statement: {}", statement);

            boolean removed = model.remove(statement);

            if (removed) {
                logger.debug("Statement deleted successfully");
                return MutationResult.success(statement, "Statement deleted");
            } else {
                logger.warn("Statement not found or deletion failed");
                return MutationResult.failure("Statement not found");
            }

        } catch (Exception e) {
            logger.error("Failed to delete statement: {}", statement, e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Failed to delete statement: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Deletes statements matching the specified subject, predicate, object in the given contexts.
     * Supports wildcards (null values).
     *
     * @param subject the subject resource (or null for wildcard).
     * @param predicate the predicate IRI (or null for wildcard).
     * @param object the object value (or null for wildcard).
     * @param contexts the array of contexts to search in.
     * @return a {@link MutationResult} summarizing all deleted statements.
     * @throws DataManagerException if the deletion process fails.
     */
    @Override
    public MutationResult deleteStatements(Resource subject, IRI predicate, Value object, Resource... contexts)
            throws DataManagerException {

        try {
            logger.debug("Deleting statements: ({}, {}, {}) in contexts: {}",
                    subject, predicate, object, contexts);

            int sizeBefore = model.size();
            boolean removed = model.remove(subject, predicate, object, contexts);
            int sizeAfter = model.size();

            int deletedCount = sizeBefore - sizeAfter;

            logger.debug("Deleted {} statement(s)", deletedCount);

            if (removed || deletedCount > 0) {
                return MutationResult.bulkBuilder()
                        .totalAttempted(deletedCount)
                        .successCount(deletedCount)
                        .message("Deleted " + deletedCount + " statement(s)")
                        .build();
            } else {
                return MutationResult.bulkBuilder()
                        .totalAttempted(0)
                        .message("No statements matched the pattern")
                        .build();
            }

        } catch (Exception e) {
            logger.error("Failed to delete statements: ({}, {}, {})", subject, predicate, object, e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Failed to delete statements: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Clears statements from the specified contexts.
     *
     * @param contexts the contexts to clear.
     * @return a {@link MutationResult} summarizing the operation.
     * @throws DataManagerException if the clear operation fails.
     */
    @Override
    public MutationResult clear(Resource... contexts) throws DataManagerException {
        try {
            logger.debug("Clearing contexts: {}", (Object[]) contexts);

            int sizeBefore = model.size();
            boolean cleared = model.clear(contexts);
            int sizeAfter = model.size();

            int deletedCount = sizeBefore - sizeAfter;

            logger.debug("Cleared {} statement(s)", deletedCount);

            if (cleared || deletedCount > 0) {
                return MutationResult.bulkBuilder()
                        .totalAttempted(deletedCount)
                        .successCount(deletedCount)
                        .message("Cleared " + deletedCount + " statement(s)")
                        .build();
            } else {
                return MutationResult.bulkBuilder()
                        .totalAttempted(0)
                        .message("No statements to clear")
                        .build();
            }

        } catch (Exception e) {
            logger.error("Failed to clear contexts", e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Failed to clear contexts: " + e.getMessage(),
                    e
            );
        }
    }
}