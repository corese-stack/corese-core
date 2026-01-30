package fr.inria.corese.core.next.datamanager.impl.operations;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.datamanager.api.operations.BulkOperations;
import fr.inria.corese.core.next.datamanager.api.support.exception.DataManagerException;
import fr.inria.corese.core.next.datamanager.api.support.exception.ErrorCode;
import fr.inria.corese.core.next.datamanager.api.support.model.StatementPattern;
import fr.inria.corese.core.next.datamanager.api.support.model.MutationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Implementation of bulk operations for CoreseModelDataManager.
 */
public class BulkOperationsImpl implements BulkOperations {

    private static final Logger logger = LoggerFactory.getLogger(BulkOperationsImpl.class);

    /** The underlying Corese model where operations are performed. */
    private final Model model;

    /**
     * Constructs a new bulk operations handler for the specified model.
     *
     * @param model the Corese Model instance to operate on; must not be null.
     * @throws IllegalArgumentException if the provided model is null.
     */
    public BulkOperationsImpl(Model model) {
        if (model == null) {
            throw new IllegalArgumentException("Model cannot be null");
        }
        this.model = model;
    }

    /**
     * Inserts a list of statements into the model in a batch.
     *
     * @param statements the list of statements to insert; must not be null or empty.
     * @return a {@link MutationResult} summarizing the successes and failures of the batch operation.
     * @throws DataManagerException if a critical system error occurs during insertion.
     * @throws IllegalArgumentException if the statements list is null or empty.
     */
    @Override
    public MutationResult insertBatch(List<Statement> statements) throws DataManagerException {
        if (statements == null || statements.isEmpty()) {
            throw new IllegalArgumentException("Statements list cannot be null or empty");
        }

        logger.info("Inserting batch of {} statements", statements.size());

        MutationResult.BulkBuilder builder = MutationResult.bulkBuilder()
                .totalAttempted(statements.size())
                .message("Batch insert of " + statements.size() + " statement(s)");

        try {
            for (Statement statement : statements) {
                try {
                    boolean added = model.add(statement);
                    if (added) {
                        builder.addSuccess(statement);
                    } else {
                        builder.addFailure(statement, "Statement already exists");
                    }
                } catch (Exception e) {
                    builder.addFailure(statement, "Insert failed: " + e.getMessage(), e);
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

    /**
     * Deletes a list of specific statements from the model.
     *
     * @param statements the list of statements to remove; must not be null or empty.
     * @return a {@link MutationResult} summarizing the statements removed and any failures.
     * @throws DataManagerException if a critical system error occurs during deletion.
     * @throws IllegalArgumentException if the statements list is null or empty.
     */
    @Override
    public MutationResult deleteBatch(List<Statement> statements) throws DataManagerException {
        if (statements == null || statements.isEmpty()) {
            throw new IllegalArgumentException("Statements list cannot be null or empty");
        }

        logger.info("Deleting batch of {} statements", statements.size());

        MutationResult.BulkBuilder builder = MutationResult.bulkBuilder()
                .totalAttempted(statements.size())
                .message("Batch delete of " + statements.size() + " statement(s)");

        try {
            for (Statement statement : statements) {
                try {
                    boolean removed = model.remove(statement);
                    if (removed) {
                        builder.addSuccess(statement);
                    } else {
                        builder.addFailure(statement, "Statement not found");
                    }
                } catch (Exception e) {
                    builder.addFailure(statement, "Delete failed: " + e.getMessage(), e);
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

    /**
     * Deletes all statements matching the specified pattern.
     *
     * @param pattern the pattern defining subject, predicate, object, and/or context filters.
     * @return a {@link MutationResult} containing the count of affected statements.
     * @throws DataManagerException if the pattern-based deletion fails.
     * @throws IllegalArgumentException if pattern is null.
     */
    @Override
    public MutationResult deleteByPattern(StatementPattern pattern) throws DataManagerException {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }

        logger.info("Deleting by pattern: {}", pattern);

        try {
            Resource subject = pattern.getSubject();
            IRI predicate = pattern.getPredicate();
            Value object = pattern.getObject();
            Resource[] contexts = pattern.getContexts();

            int sizeBefore = model.size();
            model.remove(subject, predicate, object, contexts);
            int sizeAfter = model.size();

            int deletedCount = sizeBefore - sizeAfter;

            logger.info("Deleted {} statement(s) by pattern", deletedCount);

            MutationResult.BulkBuilder builder = MutationResult.bulkBuilder()
                    .totalAttempted(deletedCount)
                    .message("Deleted " + deletedCount + " statement(s) by pattern");

            for (int i = 0; i < deletedCount; i++) {
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

    /**
     * Clears all statements from the specified contexts.
     *
     * @param contexts the contexts to clear; if null or empty, clears all statements.
     * @param silent if true, ignores missing contexts.
     * @return a {@link MutationResult} representing the clear outcome.
     * @throws DataManagerException if the operation fails at the model level.
     */
    @Override
    public MutationResult clearContexts(List<Resource> contexts, boolean silent)
            throws DataManagerException {

        logger.info("Clearing contexts: {}, silent={}", contexts, silent);

        try {
            Resource[] contextArray = (contexts != null && !contexts.isEmpty())
                    ? contexts.toArray(new Resource[0])
                    : new Resource[0];

            int sizeBefore = model.size();
            boolean success = model.clear(contextArray);
            int sizeAfter = model.size();

            int deletedCount = sizeBefore - sizeAfter;

            if (success || deletedCount > 0 || silent) {
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
            logger.error("Clear contexts failed", e);
            throw new DataManagerException(
                    ErrorCode.MUTATION_FAILED,
                    "Clear contexts failed: " + e.getMessage(),
                    e
            );
        }
    }
}