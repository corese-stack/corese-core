package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.storage.api.lifecycle.StorageLifecycle;

import java.util.Objects;
import java.util.Optional;

/**
 * Shared database state and lifecycle monitor for all in-memory sessions.
 */
final class MemoryDatabase implements StorageLifecycle {

    /**
     * Committed state whose partition references are owned by the database.
     */
    PartitionState committed = new PartitionState();

    /**
     * Committed version captured for optimistic conflict detection.
     */
    long version;

    /**
     * Current shared database lifecycle status.
     */
    LifecycleState lifecycleState = LifecycleState.NOT_INITIALIZED;

    /**
     * Configuration available during the running lifecycle.
     */
    StorageConfig config;

    /**
     * Creates an uninitialized database and its exclusively owned empty committed state.
     */
    MemoryDatabase() {
    }

    /**
     * {@inheritDoc}
     *
     * @param config storage configuration selecting this backend
     * @throws IllegalStateException if already initialized
     * @throws NullPointerException  if {@code config} is null
     */
    @Override
    public synchronized void initialize(final StorageConfig config) {
        Objects.requireNonNull(config, "config");
        if (lifecycleState != LifecycleState.NOT_INITIALIZED) {
            throw new IllegalStateException("Already initialized");
        }
        this.config = config;
        lifecycleState = LifecycleState.RUNNING;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if storage is not running
     */
    @Override
    public synchronized void shutdown() {
        if (lifecycleState != LifecycleState.RUNNING) {
            throw new IllegalStateException("Storage is not running");
        }
        committed.release();
        committed = new PartitionState();
        config = null;
        lifecycleState = LifecycleState.SHUTDOWN;
    }

    /**
     * {@inheritDoc}
     *
     * @return current lifecycle state
     */
    @Override
    public synchronized LifecycleState getState() {
        return lifecycleState;
    }

    /**
     * {@inheritDoc}
     *
     * @return current configuration when the database is running
     */
    @Override
    public synchronized Optional<StorageConfig> getConfig() {
        return Optional.ofNullable(config);
    }
}
