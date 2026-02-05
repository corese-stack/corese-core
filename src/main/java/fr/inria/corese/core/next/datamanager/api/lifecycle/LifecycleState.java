package fr.inria.corese.core.next.datamanager.api.lifecycle;

/**
 * Possible lifecycle states of the DataManager.
 */
public enum LifecycleState {
    /**
     * The DataManager is not yet initialized
     */
    NOT_INITIALIZED("Not initialized"),

    /**
     * The DataManager is initializing
     */
    INITIALIZING("Initializing"),

    /**
     * The DataManager is operational and ready
     */
    RUNNING("Running"),

    /**
     * The DataManager is shutting down
     */
    SHUTTING_DOWN("Shutting down"),

    /**
     * The DataManager has been shut down
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
     * Checks if the DataManager can be used in this state.
     *
     * @return true if usable (RUNNING)
     */
    public boolean isRunning() {
        return this == RUNNING;
    }
}
