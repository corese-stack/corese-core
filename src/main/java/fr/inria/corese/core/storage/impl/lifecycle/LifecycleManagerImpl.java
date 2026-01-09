package fr.inria.corese.core.storage.impl.lifecycle;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.storage.api.dataManager.lifecycle.DataManagerLifecycle;
import fr.inria.corese.core.storage.api.dataManager.lifecycle.LifecycleState;
import fr.inria.corese.core.storage.api.dataManager.support.config.DataManagerConfig;
import fr.inria.corese.core.storage.api.dataManager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.dataManager.support.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lifecycle manager implementation for CoreseGraphDataManager.
 * Manages initialization, state and shutdown of the DataManager.
 */
public class LifecycleManagerImpl implements DataManagerLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(LifecycleManagerImpl.class);

    // Reference to the graph to manage
    private final Graph graph;

    // Current lifecycle state (volatile for thread-safety)
    private volatile LifecycleState state;

    // Current configuration
    private DataManagerConfig config;

    // Lock for state change synchronization
    private final Object stateLock = new Object();

    /**
     * Constructs a lifecycle manager for a graph.
     *
     * @param graph Corese Graph to manage
     * @throws IllegalArgumentException if graph is null
     */
    public LifecycleManagerImpl(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }
        this.graph = graph;
        this.state = LifecycleState.NOT_INITIALIZED;
    }

    @Override
    public void initialize(DataManagerConfig config) throws DataManagerException {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }

        synchronized (stateLock) {
            // State verification
            if (state == LifecycleState.RUNNING) {
                throw new IllegalStateException("Already initialized");
            }
            if (state == LifecycleState.INITIALIZING) {
                throw new IllegalStateException("Already initializing");
            }

            logger.info("Initializing DataManager with config: {}", config);
            state = LifecycleState.INITIALIZING;

            try {
                // Save configuration
                this.config = config;

                // Initialize the graph
                graph.init();

                // Apply specific configurations
                applyConfiguration(config);

                // Move to RUNNING state
                state = LifecycleState.RUNNING;
                logger.info("DataManager initialized successfully");

            } catch (Exception e) {
                // Restore state on error
                state = LifecycleState.NOT_INITIALIZED;
                this.config = null;

                logger.error("Failed to initialize DataManager", e);
                throw new DataManagerException(
                        ErrorCode.INITIALIZATION_FAILED,
                        "Failed to initialize DataManager: " + e.getMessage(),
                        e
                );
            }
        }
    }

    @Override
    public boolean isInitialized() {
        return state == LifecycleState.RUNNING;
    }

    @Override
    public void shutdown() throws DataManagerException {
        synchronized (stateLock) {
            if (state != LifecycleState.RUNNING) {
                throw new IllegalStateException("Not running, current state: " + state);
            }

            logger.info("Shutting down DataManager");
            state = LifecycleState.SHUTTING_DOWN;

            try {
                // Clean up resources
                graph.init();

                // Move to SHUTDOWN state
                state = LifecycleState.SHUTDOWN;
                logger.info("DataManager shut down successfully");

            } catch (Exception e) {
                // On error, stay in SHUTTING_DOWN
                logger.error("Failed to shutdown DataManager cleanly", e);
                throw new DataManagerException(
                        ErrorCode.SHUTDOWN_FAILED,
                        "Failed to shutdown DataManager: " + e.getMessage(),
                        e
                );
            }
        }
    }

    @Override
    public LifecycleState getState() {
        return state;
    }

    @Override
    public DataManagerConfig getConfig() {
        return config;
    }

    /**
     * Applies the configuration to the graph.
     * This method can be extended to support more options.
     *
     * @param config Configuration to apply
     */
    private void applyConfiguration(DataManagerConfig config) {
        if (config.isDebug()) {
            logger.debug("Debug mode enabled");
        }

        // Storage path (for information)
        logger.info("Storage path: {}", config.getStoragePath());

        // Other configurations can be added here
        config.getProperties().forEach((key, value) -> logger.debug("Config property: {} = {}", key, value));
    }

    /**
     * Checks that the DataManager is in a usable state.
     * Throws an exception if not.
     *
     * @throws IllegalStateException if not in RUNNING state
     */
    public void checkUsable() {
        if (!state.isUsable()) {
            throw new IllegalStateException(
                    "DataManager is not usable, current state: " + state
            );
        }
    }
}