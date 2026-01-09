package fr.inria.corese.core.storage.api.dataManager.transaction;

public enum TransactionState {
    /**
     * Transaction is active
     */
    ACTIVE("Active"),

    /**
     * Transaction has been committed
     */
    COMMITTED("Committed"),

    /**
     * Transaction has been rolled back
     */
    ROLLED_BACK("Rolled back"),

    /**
     * Transaction is in error
     */
    FAILED("Failed");

    private final String description;

    TransactionState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if transaction can still be used.
     *
     * @return true if active
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
}