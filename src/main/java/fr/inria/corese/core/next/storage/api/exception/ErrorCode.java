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

    /** Storage plugin discovery, selection, or creation failure */
    PLUGIN_FAILED("PLUGIN_FAIL", "Storage plugin failure"),
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
