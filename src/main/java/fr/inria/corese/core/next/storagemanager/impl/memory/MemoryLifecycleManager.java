package fr.inria.corese.core.next.storagemanager.impl.memory;

import fr.inria.corese.core.next.storagemanager.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.storagemanager.api.lifecycle.StorageLifecycle;
import fr.inria.corese.core.next.storagemanager.api.support.config.StorageConfig;
import fr.inria.corese.core.next.storagemanager.api.support.exception.ErrorCode;
import fr.inria.corese.core.next.storagemanager.api.support.exception.StorageException;

/**
 * Lifecycle manager for {@link MemoryStorageManager}.
 */
public class MemoryLifecycleManager implements StorageLifecycle {

    private volatile LifecycleState state = LifecycleState.NOT_INITIALIZED;
    private volatile StorageConfig config;

    /**
     * Constructs a new MemoryLifecycleManager.
     *
     * @param adapter the MemoryAdapter to manage (must not be null)
     * @throws IllegalArgumentException if adapter is null
     */
    public MemoryLifecycleManager(MemoryAdapter adapter) {
        if (adapter == null) throw new IllegalArgumentException("MemoryAdapter cannot be null");
    }

    /**
     * Initializes the MemoryStorageManager with the given configuration.
     *
     * @param config the storage configuration (must not be null)
     * @throws IllegalArgumentException if config is null
     * @throws IllegalStateException    if already initialized
     * @throws StorageException         if initialization fails
     */
    @Override
    public void initialize(StorageConfig config) throws StorageException {
        if (config == null) throw new IllegalArgumentException("Config cannot be null");
        if (state != LifecycleState.NOT_INITIALIZED) {
            throw new IllegalStateException("Already initialized");
        }

        try {
            this.config = config;
            this.state = LifecycleState.RUNNING;
        } catch (Exception e) {
            this.state = LifecycleState.NOT_INITIALIZED;
            throw new StorageException(ErrorCode.INITIALIZATION_FAILED, "Init failed", e);
        }
    }

    /**
     * Shuts down the MemoryStorageManager.
     *
     * @throws IllegalStateException if not currently running
     * @throws StorageException      if shutdown fails
     */
    @Override
    public void shutdown() throws StorageException {
        if (state != LifecycleState.RUNNING) {
            throw new IllegalStateException("Not running");
        }
        // Note: We don't clear data on shutdown - caller must do that explicitly if desired
        this.state = LifecycleState.SHUTDOWN;
        this.config = null;
    }

    /**
     * Returns the current lifecycle state.
     *
     * @return the current state (never null)
     */
    @Override
    public LifecycleState getState() {
        return state;
    }

    /**
     * Returns the current configuration.
     *
     * @return the config if initialized/running, null otherwise
     */
    @Override
    public StorageConfig getConfig() {
        return config;
    }
}
