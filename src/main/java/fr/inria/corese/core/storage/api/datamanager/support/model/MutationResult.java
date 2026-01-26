package fr.inria.corese.core.storage.api.datamanager.support.model;

import fr.inria.corese.core.kgram.api.core.Edge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Unified result for mutation operations.
 *
 */
public final class MutationResult {

    private final boolean isBulk;
    private final boolean success;
    private final List<Edge> affectedEdges;
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
    private MutationResult(boolean isBulk, boolean success, List<Edge> affectedEdges,
                           String message, Throwable error,
                           int totalAttempted, int successCount, int failureCount,
                           List<MutationError> errors) {
        this.isBulk = isBulk;
        this.success = success;
        this.affectedEdges = affectedEdges != null ?
                List.copyOf(affectedEdges) :
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
     * @param affectedEdge The edge that was affected (can be null for operations without edges)
     * @return Successful result
     */
    public static MutationResult success(Edge affectedEdge) {
        return new MutationResult(
                false,                          // not bulk
                true,                           // success
                affectedEdge != null ? List.of(affectedEdge) : Collections.emptyList(),
                "Success",                      // message
                null,                           // no error
                1, 1, 0,                       // attempted=1, success=1, failure=0
                null                            // no errors list
        );
    }

    /**
     * Creates a successful single-operation result with custom message.
     *
     * @param affectedEdge The edge that was affected (can be null for operations without edges)
     * @param message      Success message
     * @return Successful result
     */
    public static MutationResult success(Edge affectedEdge, String message) {
        return new MutationResult(
                false, true,
                affectedEdge != null ? List.of(affectedEdge) : Collections.emptyList(),
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
     * Returns the single affected edge.
     * Convenience method for single operations.
     *
     * @return The affected edge, or null if none or if bulk operation
     * @throws IllegalStateException if called on bulk result with multiple edges
     */
    public Edge getAffectedEdge() {
        if (affectedEdges.isEmpty()) {
            return null;
        }
        if (affectedEdges.size() == 1) {
            return affectedEdges.getFirst();
        }
        throw new IllegalStateException(
                "Multiple edges affected (" + affectedEdges.size() +
                        "). Use getAffectedEdges() for bulk operations."
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
                return "MutationResult{success=true, edge=" + getAffectedEdge() +
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
        private final Edge edge;
        private final String message;
        private final Throwable cause;

        public MutationError(Edge edge, String message) {
            this(edge, message, null);
        }

        public MutationError(Edge edge, String message, Throwable cause) {
            this.edge = edge;
            this.message = message;
            this.cause = cause;
        }

        public Edge getEdge() {
            return edge;
        }

        public String getMessage() {
            return message;
        }

        public Throwable getCause() {
            return cause;
        }

        @Override
        public String toString() {
            return "MutationError{edge=" + edge + ", message='" + message + "'}";
        }
    }

    /**
     * Builder for bulk operation results.
     */
    public static class BulkBuilder {
        private int totalAttempted = 0;
        private int successCount = 0;
        private int failureCount = 0;
        private final List<Edge> affectedEdges = new ArrayList<>();
        private final List<MutationError> errors = new ArrayList<>();
        private String message = "Bulk operation completed";

        private BulkBuilder() {
        }

        public BulkBuilder totalAttempted(int total) {
            this.totalAttempted = total;
            return this;
        }

        public BulkBuilder message(String message) {
            this.message = message;
            return this;
        }

        public BulkBuilder incrementSuccess() {
            this.successCount++;
            return this;
        }

        public BulkBuilder incrementFailure() {
            this.failureCount++;
            return this;
        }

        public void addSuccess(Edge edge) {
            if (edge != null) {
                this.affectedEdges.add(edge);
            }
            this.successCount++;
        }

        public void addFailure(Edge edge, String errorMessage) {
            this.errors.add(new MutationError(edge, errorMessage));
            this.failureCount++;
        }

        public void addFailure(Edge edge, String errorMessage, Throwable cause) {
            this.errors.add(new MutationError(edge, errorMessage, cause));
            this.failureCount++;
        }

        public BulkBuilder addAffectedEdges(List<Edge> edges) {
            if (edges != null) {
                this.affectedEdges.addAll(edges);
            }
            return this;
        }

        public MutationResult build() {
            boolean success = failureCount == 0 && successCount == totalAttempted;
            return new MutationResult(
                    true,                    // is bulk
                    success,                 // overall success
                    affectedEdges,          // affected edges
                    message,                // message
                    null,                   // no single error
                    totalAttempted,
                    successCount,
                    failureCount,
                    errors
            );
        }
    }
}