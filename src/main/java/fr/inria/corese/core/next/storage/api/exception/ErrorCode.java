package fr.inria.corese.core.next.storage.api.exception;

/**
 * Stable error codes for storage operations.
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
            "Restart failed and unable to restore previous configuration"),

    /**
     * Plugin failed to create StorageManager instance
     */
    PLUGIN_CREATION_FAILED("PLUGIN_CREATION_FAILED", "Plugin failed to create StorageManager instance"),
    ;

    private final String code;
    private final String description;

    ErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public String description() {
        return description;
    }

}
