package fr.inria.corese.core.next.datamanager.api.transaction;

/**
 * Transaction isolation levels.
 */
public enum IsolationLevel {

    /**
     * Read Uncommitted.
     */
    READ_UNCOMMITTED(1),

    /**
     * Read Committed.
     */
    READ_COMMITTED(2),

    /**
     * Repeatable Read.
     * Guarantees that if a transaction reads data multiple times,
     * it will always get the same value.
     */
    REPEATABLE_READ(3),

    /**
     * Serializable.
     * Strictest isolation level.
     * Transactions execute as if they were sequential.
     */
    SERIALIZABLE(4);

    private final int level;

    IsolationLevel(int level) {
        this.level = level;
    }

    /**
     * Returns the numeric isolation level.
     * Higher number means stricter isolation.
     *
     * @return Numeric level
     */
    public int getLevel() {
        return level;
    }

}