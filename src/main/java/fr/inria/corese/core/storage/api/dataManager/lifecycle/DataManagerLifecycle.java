package fr.inria.corese.core.storage.api.dataManager.lifecycle;

import fr.inria.corese.core.storage.api.dataManager.support.config.DataManagerConfig;
import fr.inria.corese.core.storage.api.dataManager.support.exception.DataManagerException;

/**
 * Lifecycle management for the DataManager.
 */
public interface DataManagerLifecycle {

    /**
     * Initializes the DataManager with the provided configuration.
     *
     * @param config DataManager configuration (must not be null)
     * @throws DataManagerException     if initialization fails
     * @throws IllegalStateException    if already initialized
     * @throws IllegalArgumentException if config is null
     */
    void initialize(DataManagerConfig config) throws DataManagerException;

    /**
     * Checks if the DataManager is initialized and ready to use.
     *
     * @return true if initialized (RUNNING state), false otherwise
     */
    boolean isInitialized();

    /**
     * Cleanly shuts down the DataManager and releases all resources.
     *
     * @throws DataManagerException  if shutdown fails
     * @throws IllegalStateException if not initialized
     */
    void shutdown() throws DataManagerException;

    /**
     * Restarts the DataManager with a new configuration.
     *
     * @param config New configuration
     * @throws DataManagerException if restart fails
     */
    default void restart(DataManagerConfig config) throws DataManagerException {
        if (isInitialized()) {
            shutdown();
        }
        initialize(config);
    }

    /**
     * Returns the current lifecycle state.
     *
     * @return Current state
     */
    LifecycleState getState();

    /**
     * Returns the currently used configuration.
     *
     * @return Current configuration, or null if not initialized
     */
    DataManagerConfig getConfig();


}