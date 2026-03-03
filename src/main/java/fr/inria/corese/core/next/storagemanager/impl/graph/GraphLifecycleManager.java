package fr.inria.corese.core.next.storagemanager.impl.graph;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.next.storagemanager.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.storagemanager.api.lifecycle.StorageLifecycle;
import fr.inria.corese.core.next.storagemanager.api.support.config.StorageConfig;
import fr.inria.corese.core.next.storagemanager.api.support.exception.ErrorCode;
import fr.inria.corese.core.next.storagemanager.api.support.exception.StorageException;

/**
 * Lifecycle manager for {@link GraphStorageManager}.
 */
public class GraphLifecycleManager implements StorageLifecycle {

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
    public void initialize(StorageConfig config) throws StorageException {
        if (config == null) throw new IllegalArgumentException("Config cannot be null");
        if (state != LifecycleState.NOT_INITIALIZED) {
            throw new IllegalStateException("Already initialized");
        }

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
    public void shutdown() throws StorageException {
        if (state != LifecycleState.RUNNING) {
            throw new IllegalStateException("Not running");
        }
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
