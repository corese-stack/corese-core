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
 * Implementation for Corese Graph Data Management.
 * This class is responsible for controlling the transitions between different stages
 * of the DataManager's life, including initialization, operational state, and shutdown.
 * It ensures thread-safe state transitions using internal synchronization.
 */
public class LifecycleManagerImpl implements DataManagerLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(LifecycleManagerImpl.class);

    /** Reference to the Corese Graph instance being managed. */
    private final Graph graph;

    /** * The current state of the lifecycle.
     * Marked as {@code volatile} to ensure visibility across different threads.
     */
    private volatile LifecycleState state;

    /** The configuration applied to the DataManager during initialization. */
    private DataManagerConfig config;

    /** Lock object used to synchronize state-changing operations. */
    private final Object stateLock = new Object();

    /**
     * Constructs a new LifecycleManager for the specified graph.
     * Initial state is set to {@link LifecycleState#NOT_INITIALIZED}.
     *
     * @param graph the Corese Graph to be managed; must not be null.
     * @throws IllegalArgumentException if the provided graph is null.
     */
    public LifecycleManagerImpl(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }
        this.graph = graph;
        this.state = LifecycleState.NOT_INITIALIZED;
    }

    /**
     * Initializes the DataManager with the provided configuration.
     * This method transitions the state from {@code NOT_INITIALIZED} to {@code INITIALIZING},
     * performs the setup logic, and finally moves to the {@code RUNNING} state.
     *
     * @param config the configuration settings to apply; must not be null.
     * @throws DataManagerException if an error occurs during the graph initialization process.
     * @throws IllegalStateException if the manager is already initialized or currently initializing.
     * @throws IllegalArgumentException if the provided config is null.
     */
    @Override
    public void initialize(DataManagerConfig config) throws DataManagerException {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }

        synchronized (stateLock) {
            // State verification
            if (state == LifecycleState.RUNNING) {
                throw new IllegalStateException("DataManager is already initialized");
            }
            if (state == LifecycleState.INITIALIZING) {
                throw new IllegalStateException("DataManager is already in the process of initializing");
            }

            logger.info("Initializing DataManager with config: {}", config);
            state = LifecycleState.INITIALIZING;

            try {
                // Save configuration
                this.config = config;

                // Initialize the underlying Corese graph
                graph.init();

                // Apply specific configuration settings
                applyConfiguration(config);

                // Transition to RUNNING state
                state = LifecycleState.RUNNING;
                logger.info("DataManager initialized successfully");

            } catch (Exception e) {
                // Rollback state on failure
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

    /**
     * Checks if the DataManager has been successfully initialized and is currently running.
     *
     * @return {@code true} if the current state is {@link LifecycleState#RUNNING}, {@code false} otherwise.
     */
    @Override
    public boolean isInitialized() {
        return state == LifecycleState.RUNNING;
    }

    /**
     * Shuts down the DataManager and releases associated resources.
     * Transition logic: {@code RUNNING} -> {@code SHUTTING_DOWN} -> {@code SHUTDOWN}.
     *
     * @throws DataManagerException if an error occurs during the shutdown process.
     * @throws IllegalStateException if the DataManager is not in a RUNNING state.
     */
    @Override
    public void shutdown() throws DataManagerException {
        synchronized (stateLock) {
            if (state != LifecycleState.RUNNING) {
                throw new IllegalStateException("DataManager cannot be shut down because it is in state: " + state);
            }

            logger.info("Shutting down DataManager");
            state = LifecycleState.SHUTTING_DOWN;

            try {
                // Perform cleanup (re-init often acts as a reset/cleanup in Corese Graph)
                graph.init();

                // Transition to final SHUTDOWN state
                state = LifecycleState.SHUTDOWN;
                logger.info("DataManager shut down successfully");

            } catch (Exception e) {
                // Remain in SHUTTING_DOWN state to indicate a partial or failed shutdown
                logger.error("Failed to shutdown DataManager cleanly", e);
                throw new DataManagerException(
                        ErrorCode.SHUTDOWN_FAILED,
                        "Failed to shutdown DataManager: " + e.getMessage(),
                        e
                );
            }
        }
    }

    /**
     * Returns the current lifecycle state of the DataManager.
     *
     * @return the current
     */
    @Override
    public LifecycleState getState() {
        return state;
    }

    /**
     * Retrieves the configuration used to initialize the DataManager.
     *
     * @return the {@link DataManagerConfig} instance, or {@code null} if not yet initialized.
     */
    @Override
    public DataManagerConfig getConfig() {
        return config;
    }

    /**
     * Internal helper method to apply configuration parameters to the managed graph.
     * Logs debug information and storage paths.
     *
     * @param config the configuration to apply.
     */
    private void applyConfiguration(DataManagerConfig config) {
        if (config.isDebug()) {
            logger.debug("Debug mode enabled");
        }

        // Log storage path information
        logger.info("Storage path: {}", config.getStoragePath());

        // Other configurations can be added here
        config.getProperties().forEach((key, value) -> logger.debug("Config property: {} = {}", key, value));
    }

    /**
     * Validates that the DataManager is in a state where it can accept data operations.
     * Use this method before any read/write access to the underlying storage.
     *
     * @throws IllegalStateException if the current state is not usable (e.g., NOT_INITIALIZED or SHUTDOWN).
     */
    public void checkUsable() {
        if (!state.isUsable()) {
            throw new IllegalStateException(
                    "DataManager is not in a usable state. Current state: " + state
            );
        }
    }
}