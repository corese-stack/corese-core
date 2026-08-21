package fr.inria.corese.core.next.storage.api.lifecycle;

/**
 * Possible lifecycle states of the StorageManager.
 */
public enum LifecycleState {
    /**
     * The StorageManager is not yet initialized
     */
    NOT_INITIALIZED("Not initialized"),

    /**
     * The StorageManager is initializing
     */
    INITIALIZING("Initializing"),

    /**
     * The StorageManager is operational and ready
     */
    RUNNING("Running"),

    /**
     * The StorageManager is shutting down
     */
    SHUTTING_DOWN("Shutting down"),

    /**
     * The StorageManager has been shut down
     */
    SHUTDOWN("Shutdown");

    private final String description;

    LifecycleState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if the StorageManager can be used in this state.
     *
     * @return true if usable (RUNNING)
     */
    public boolean isRunning() {
        return this == RUNNING;
    }
}
