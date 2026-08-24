package fr.inria.corese.core.next.storage.impl.graph;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.next.storage.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.storage.api.lifecycle.StorageLifecycle;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.exception.ErrorCode;
import fr.inria.corese.core.next.storage.api.exception.StorageException;

import java.util.Optional;

/**
 * Lifecycle manager for {@link GraphStorageManager}.
 */
@SuppressWarnings("java:S3077")
final class GraphLifecycleManager implements StorageLifecycle {

    private final Graph graph;
    private volatile LifecycleState state = LifecycleState.NOT_INITIALIZED;
    private volatile StorageConfig config;

    /**
     * Constructs a new GraphLifecycleManager.
     *
     * @param graph the Graph to manage (must not be null)
     * @throws IllegalArgumentException if graph is null
     */
    public GraphLifecycleManager(Graph graph) {
        if (graph == null) throw new IllegalArgumentException("Graph cannot be null");
        this.graph = graph;
    }

    /**
     * Initializes the Graph with the given configuration.
     *
     * @param config the storage configuration (must not be null)
     * @throws IllegalArgumentException if config is null
     * @throws IllegalStateException    if already initialized
     * @throws StorageException         if Graph initialization fails
     */
    @Override
    public synchronized void initialize(StorageConfig config) throws StorageException {
        if (config == null) throw new IllegalArgumentException("Config cannot be null");
        if (state != LifecycleState.NOT_INITIALIZED) {
            throw new IllegalStateException("Already initialized");
        }

        this.state = LifecycleState.INITIALIZING;
        try {
            graph.init();
            this.config = config;
            this.state = LifecycleState.RUNNING;
        } catch (Exception e) {
            this.state = LifecycleState.NOT_INITIALIZED;
            throw new StorageException(ErrorCode.INITIALIZATION_FAILED, "Init failed", e);
        }
    }

    /**
     * Shuts down the Graph.
     *
     * @throws IllegalStateException if not currently running
     * @throws StorageException      if shutdown fails
     */
    @Override
    public synchronized void shutdown() throws StorageException {
        if (state != LifecycleState.RUNNING) {
            throw new IllegalStateException("Not running");
        }
        this.state = LifecycleState.SHUTTING_DOWN;
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
     * @return the config if initialized/running, empty otherwise
     */
    @Override
    public Optional<StorageConfig> getConfig() {
        return Optional.ofNullable(config);
    }
}
