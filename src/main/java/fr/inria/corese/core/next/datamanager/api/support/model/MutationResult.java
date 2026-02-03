package fr.inria.corese.core.next.datamanager.api.support.model;

import fr.inria.corese.core.next.data.api.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Unified result for mutation operations.
 */
public final class MutationResult {

    private final boolean isBulk;
    private final boolean success;
    private final List<Statement> affectedStatements;
    private final String message;
    private final Throwable error;

    // Bulk-specific fields
    private final int totalAttempted;
    private final int successCount;
    private final int failureCount;
    private final List<MutationError> errors;

    /**
     * Private constructor - use factory methods or Builder.
     */
    private MutationResult(boolean isBulk, boolean success, List<Statement> affectedStatements,
                           String message, Throwable error,
                           int totalAttempted, int successCount, int failureCount,
                           List<MutationError> errors) {
        this.isBulk = isBulk;
        this.success = success;
        this.affectedStatements = affectedStatements != null ?
                List.copyOf(affectedStatements) :
                Collections.emptyList();
        this.message = message;
        this.error = error;
        this.totalAttempted = totalAttempted;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.errors = errors != null ?
                List.copyOf(errors) :
                Collections.emptyList();
    }

    /**
     * Creates a successful single-operation result.
     *
     * @param affectedStatement The statement that was affected (can be null)
     * @return Successful result
     */
    public static MutationResult success(Statement affectedStatement) {
        return new MutationResult(
                false,                          // not bulk
                true,                           // success
                affectedStatement != null ? List.of(affectedStatement) : Collections.emptyList(),
                "Success",                      // message
                null,                           // no error
                1, 1, 0,                       // attempted=1, success=1, failure=0
                null                            // no errors list
        );
    }

    /**
     * Creates a successful single-operation result with custom message.
     *
     * @param affectedStatement The statement that was affected (can be null)
     * @param message           Success message
     * @return Successful result
     */
    public static MutationResult success(Statement affectedStatement, String message) {
        return new MutationResult(
                false, true,
                affectedStatement != null ? List.of(affectedStatement) : Collections.emptyList(),
                message, null,
                1, 1, 0, null
        );
    }

    /**
     * Creates a failure result for single operation.
     *
     * @param message Failure message
     * @return Failed result
     */
    public static MutationResult failure(String message) {
        return new MutationResult(
                false, false, Collections.emptyList(), message, null,
                1, 0, 1, null
        );
    }

    /**
     * Creates a failure result with error for single operation.
     *
     * @param message Failure message
     * @param error   The error that caused the failure
     * @return Failed result
     */
    public static MutationResult failure(String message, Throwable error) {
        return new MutationResult(
                false, false, Collections.emptyList(), message, error,
                1, 0, 1, null
        );
    }

    /**
     * Creates a builder for bulk operations.
     *
     * @return New builder instance for bulk results
     */
    public static BulkBuilder bulkBuilder() {
        return new BulkBuilder();
    }

    /**
     * Checks if this is a bulk operation result.
     *
     * @return true if bulk, false if single
     */
    public boolean isBulk() {
        return isBulk;
    }

    /**
     * Checks if the operation was successful.
     * For bulk: true if ALL operations succeeded.
     * For single: true if the operation succeeded.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Checks if the operation failed.
     *
     * @return true if failed
     */
    public boolean isFailure() {
        return !success;
    }

    /**
     * Returns the result message.
     *
     * @return Result message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the error (if any).
     *
     * @return Optional containing error, or empty if no error
     */
    public Optional<Throwable> getError() {
        return Optional.ofNullable(error);
    }

    /**
     * Returns the single affected statement.
     * Convenience method for single operations.
     *
     * @return The affected statement, or null if none or if bulk operation
     * @throws IllegalStateException if called on bulk result with multiple statements
     */
    public Statement getAffectedStatement() {
        if (affectedStatements.isEmpty()) {
            return null;
        }
        if (affectedStatements.size() == 1) {
            return affectedStatements.getFirst();
        }
        throw new IllegalStateException(
                "Multiple statements affected (" + affectedStatements.size() +
                        "). Use getAffectedStatements() for bulk operations."
        );
    }

    /**
     * Returns the total number of operations attempted.
     * For single operations: always 1.
     *
     * @return Total attempted
     */
    public int getTotalAttempted() {
        return totalAttempted;
    }

    /**
     * Returns the number of successful operations.
     * For single operations: 1 if success, 0 if failure.
     *
     * @return Success count
     */
    public int getSuccessCount() {
        return successCount;
    }

    /**
     * Returns the number of failed operations.
     * For single operations: 0 if success, 1 if failure.
     *
     * @return Failure count
     */
    public int getFailureCount() {
        return failureCount;
    }

    /**
     * Returns all errors that occurred (bulk operations only).
     * For single operations: empty list (use getError() instead).
     *
     * @return Unmodifiable list of errors
     */
    public List<MutationError> getErrors() {
        return errors;
    }

    /**
     * Checks if all operations were successful (bulk).
     * For single operations: same as isSuccess().
     *
     * @return true if all successful
     */
    public boolean isCompleteSuccess() {
        return failureCount == 0 && successCount == totalAttempted;
    }

    /**
     * Returns the success rate (0.0 to 1.0).
     *
     * @return Success rate
     */
    public double getSuccessRate() {
        if (totalAttempted == 0) {
            return 0.0;
        }
        return (double) successCount / totalAttempted;
    }

    @Override
    public String toString() {
        if (isBulk) {
            return "MutationResult{bulk=true, attempted=" + totalAttempted +
                    ", success=" + successCount + ", failure=" + failureCount +
                    ", successRate=" + String.format("%.2f%%", getSuccessRate() * 100) + "}";
        } else {
            if (success) {
                return "MutationResult{success=true, statement=" + getAffectedStatement() +
                        ", message='" + message + "'}";
            } else {
                return "MutationResult{success=false, message='" + message + "'" +
                        (error != null ? ", error=" + error.getClass().getSimpleName() : "") + "}";
            }
        }
    }

    /**
     * Represents an error in a bulk operation.
     */
    public static class MutationError {
        private final Statement statement;
        private final String message;
        private final Throwable cause;

        public MutationError(Statement statement, String message) {
            this(statement, message, null);
        }

        public MutationError(Statement statement, String message, Throwable cause) {
            this.statement = statement;
            this.message = message;
            this.cause = cause;
        }

        public Statement getStatement() {
            return statement;
        }

        public String getMessage() {
            return message;
        }

        public Throwable getCause() {
            return cause;
        }

        @Override
        public String toString() {
            return "MutationError{statement=" + statement + ", message='" + message + "'}";
        }
    }

    /**
     * Builder for bulk operation results.
     */
    public static class BulkBuilder {
        private int totalAttempted = 0;
        private int successCount = 0;
        private int failureCount = 0;
        private final List<Statement> affectedStatements = new ArrayList<>();
        private final List<MutationError> errors = new ArrayList<>();
        private String message = "Bulk operation completed";

        private BulkBuilder() {
        }

        public BulkBuilder totalAttempted(int total) {
            this.totalAttempted = total;
            return this;
        }

        public BulkBuilder successCount(int count) {
            this.successCount = count;
            return this;
        }

        public BulkBuilder failureCount(int count) {
            this.failureCount = count;
            return this;
        }

        public BulkBuilder message(String message) {
            this.message = message;
            return this;
        }

        public void incrementSuccess() {
            this.successCount++;
        }


        public BulkBuilder addSuccess(Statement statement) {
            if (statement != null) {
                this.affectedStatements.add(statement);
            }
            this.successCount++;
            return this;
        }

        public BulkBuilder addFailure(Statement statement, String errorMessage) {
            this.errors.add(new MutationError(statement, errorMessage));
            this.failureCount++;
            return this;
        }

        public BulkBuilder addFailure(Statement statement, String errorMessage, Throwable cause) {
            this.errors.add(new MutationError(statement, errorMessage, cause));
            this.failureCount++;
            return this;
        }

        public MutationResult build() {
            boolean success = failureCount == 0 && successCount == totalAttempted;
            return new MutationResult(
                    true,                    // is bulk
                    success,                 // overall success
                    affectedStatements,      // affected statements
                    message,                 // message
                    null,                    // no single error
                    totalAttempted,
                    successCount,
                    failureCount,
                    errors
            );
        }
    }
}