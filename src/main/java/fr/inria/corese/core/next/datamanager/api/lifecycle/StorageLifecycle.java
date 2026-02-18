package fr.inria.corese.core.next.datamanager.api.lifecycle;

import fr.inria.corese.core.next.datamanager.api.support.config.StorageConfig;
import fr.inria.corese.core.next.datamanager.api.support.exception.StorageException;

/**
 * Manages the lifecycle of a StorageManager instance.
 */
public interface StorageLifecycle {

    /**
     * Initializes the storage backend with the provided configuration.
     *
     * @param config the storage configuration (must not be {@code null})
     * @throws StorageException         if initialization fails
     * @throws IllegalStateException    if the storage is not in {@code NOT_INITIALIZED} state
     * @throws IllegalArgumentException if {@code config} is {@code null}
     */
    void initialize(StorageConfig config) throws StorageException;

    /**
     * Cleanly shuts down the storage backend and releases all resources.
     *
     * @throws StorageException      if shutdown fails
     * @throws IllegalStateException if the storage is not in {@code RUNNING} state
     */
    void shutdown() throws StorageException;

    /**
     * Returns the current lifecycle state of the storage.
     *
     * @return the current {@link LifecycleState} (never {@code null})
     */
    LifecycleState getState();

    /**
     * Returns the configuration used to initialize the storage.
     *
     * @return the {@link StorageConfig}, or {@code null} if not yet initialized
     */
    StorageConfig getConfig();

    /**
     * Checks whether the storage has been successfully initialized and is running.
     *
     * @return {@code true} if the storage is in {@link LifecycleState#RUNNING} state
     */
    default boolean isInitialized() {
        return getState() == LifecycleState.RUNNING;
    }
}