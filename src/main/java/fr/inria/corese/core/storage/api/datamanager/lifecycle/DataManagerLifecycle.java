package fr.inria.corese.core.storage.api.datamanager.lifecycle;

import fr.inria.corese.core.storage.api.datamanager.support.config.DataManagerConfig;
import fr.inria.corese.core.storage.api.datamanager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.datamanager.support.exception.ErrorCode;

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
    default boolean isInitialized() {
        return getState().isRunnig();
    }

    /**
     * Cleanly shuts down the DataManager and releases all resources.
     *
     * @throws DataManagerException  if shutdown fails
     * @throws IllegalStateException if not initialized
     */
    void shutdown() throws DataManagerException;

    /**
     * Restarts the DataManager with a new configuration.
     * This method attempts to restore the previous configuration if the restart fails.
     * However, restoration is not guaranteed if both shutdown and re-initialization fail.
     *
     * @param config New configuration (must not be null)
     * @throws DataManagerException     if restart fails
     * @throws IllegalArgumentException if config is null
     * @throws IllegalStateException    if shutdown fails (when already initialized)
     */
    default void restart(DataManagerConfig config) throws DataManagerException {
        DataManagerConfig oldConfig = getConfig();
        boolean wasInitialized = isInitialized();

        if (wasInitialized) {
            shutdown();
        }

        try {
            initialize(config);
        } catch (DataManagerException e) {
            if (wasInitialized && oldConfig != null) {
                try {
                    initialize(oldConfig);
                    throw new DataManagerException(
                            ErrorCode.RESTART_FAILED_ROLLBACK_SUCCESS,
                            "Failed to restart with new config. Restored previous configuration.",
                            e);
                } catch (DataManagerException rollbackEx) {
                    DataManagerException criticalFailure = new DataManagerException(
                            ErrorCode.RESTART_FAILED_ROLLBACK_FAILED,
                            "Failed to restart and unable to restore previous configuration.",
                            e);
                    criticalFailure.addSuppressed(rollbackEx);
                    throw criticalFailure;
                }
            }
            throw e;
        }
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