package fr.inria.corese.core.next.storage.api.transaction;

/**
 * Transaction isolation levels.
 */
public enum IsolationLevel {

    /**
     * Read Uncommitted.
     */
    READ_UNCOMMITTED,

    /**
     * Read Committed.
     */
    READ_COMMITTED,

    /**
     * Repeatable Read.
     * Guarantees that if a transaction reads data multiple times,
     * it will always get the same value.
     */
    REPEATABLE_READ,

    /**
     * Serializable.
     * Strictest isolation level.
     * Transactions execute as if they were sequential.
     */
    SERIALIZABLE

}
