package fr.inria.corese.core.next.storagemanager.api.support.exception;

/**
 * Error codes for DataManager operations.
 */
public enum ErrorCode {
    /** Initialization failure */
    INITIALIZATION_FAILED("INIT_FAIL", "Initialization failed"),

    /** Shutdown failure */
    SHUTDOWN_FAILED("SHUTDOWN_FAIL", "Shutdown failed"),

    /** Transaction error */
    TRANSACTION_ERROR("TX_ERROR", "Transaction error"),

    /** Query failure */
    QUERY_FAILED("QUERY_FAIL", "Query failed"),

    /** Mutation failure */
    MUTATION_FAILED("MUTATION_FAIL", "Mutation failed"),

    /** Invalid pattern */
    INVALID_PATTERN("INVALID_PATTERN", "Invalid pattern"),

    /** Context not found */
    CONTEXT_NOT_FOUND("CTX_NOT_FOUND", "Context not found"),

    /** Unsupported operation */
    UNSUPPORTED_OPERATION("UNSUPPORTED", "Operation not supported"),

    /** Invalid state */
    INVALID_STATE("INVALID_STATE", "Invalid state"),

    RESTART_FAILED_ROLLBACK_SUCCESS("RESTART_FAIL_ROLLBACK_OK",
            "Restart failed but previous configuration restored"),

    /** Restart failed and rollback also failed (critical) */
    RESTART_FAILED_ROLLBACK_FAILED("RESTART_FAIL_ROLLBACK_FAIL",
            "Restart failed and unable to restore previous configuration"),;

    private final String code;
    private final String description;

    ErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
