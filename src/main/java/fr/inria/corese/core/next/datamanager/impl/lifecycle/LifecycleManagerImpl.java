package fr.inria.corese.core.next.datamanager.impl.lifecycle;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.datamanager.api.lifecycle.DataManagerLifecycle;
import fr.inria.corese.core.next.datamanager.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.datamanager.api.support.config.DataManagerConfig;
import fr.inria.corese.core.next.datamanager.api.support.exception.DataManagerException;
import fr.inria.corese.core.next.datamanager.api.support.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of lifecycle management for CoreseModelDataManager.
 * Handles initialization, shutdown, and restart operations with proper state management.
 */
public class LifecycleManagerImpl implements DataManagerLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(LifecycleManagerImpl.class);

    /**
     * The underlying Corese Model instance.
     */
    private final Model model;

    /**
     * Current lifecycle state. Volatile ensures visibility across threads.
     */
    private volatile LifecycleState state;

    /**
     * Current configuration. Null until initialized.
     */
    private volatile DataManagerConfig config;

    /**
     * Lock object for thread-safe state transitions.
     */
    private final Object stateLock = new Object();

    /**
     * Constructs a new lifecycle manager.
     *
     * @param model the Corese Model to manage; must not be null.
     * @throws IllegalArgumentException if model is null.
     */
    public LifecycleManagerImpl(Model model) {
        if (model == null) {
            throw new IllegalArgumentException("Model cannot be null");
        }
        this.model = model;
        this.state = LifecycleState.NOT_INITIALIZED;
        this.config = null;

        logger.debug("LifecycleManager created in state: {}", state);
    }

    /**
     * Initializes the DataManager with the provided configuration.
     *
     * @param config DataManager configuration (must not be null)
     * @throws DataManagerException     if initialization fails
     * @throws IllegalStateException    if already initialized
     * @throws IllegalArgumentException if config is null
     */
    @Override
    public void initialize(DataManagerConfig config) throws DataManagerException {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        synchronized (stateLock) {
            if (state != LifecycleState.NOT_INITIALIZED) {
                throw new IllegalStateException(
                        "Cannot initialize: current state is " + state +
                                ". DataManager must be in NOT_INITIALIZED state."
                );
            }

            setState(LifecycleState.INITIALIZING);
        }

        try {
            logger.info("Initializing DataManager with config: {}", config);

            // Store configuration
            this.config = config;

            // Model doesn't have an init() method
            // So we just validate the model is ready
            if (model == null) {
                throw new DataManagerException(
                        ErrorCode.INITIALIZATION_FAILED,
                        "Model is null - cannot initialize"
                );
            }

            // Optional: Perform any custom initialization logic here
            synchronized (stateLock) {
                setState(LifecycleState.RUNNING);
            }

            logger.info("DataManager initialized successfully. State: {}", state);

        } catch (Exception e) {
            logger.error("Initialization failed", e);

            // Rollback to NOT_INITIALIZED on failure
            synchronized (stateLock) {
                this.config = null;
                setState(LifecycleState.NOT_INITIALIZED);
            }

            if (e instanceof DataManagerException) {
                throw (DataManagerException) e;
            }

            throw new DataManagerException(
                    ErrorCode.INITIALIZATION_FAILED,
                    "Initialization failed: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Cleanly shuts down the DataManager and releases all resources.
     *
     * @throws DataManagerException  if shutdown fails
     * @throws IllegalStateException if not initialized
     */
    @Override
    public void shutdown() throws DataManagerException {
        synchronized (stateLock) {
            if (state != LifecycleState.RUNNING) {
                throw new IllegalStateException(
                        "Cannot shutdown: current state is " + state +
                                ". DataManager must be in RUNNING state."
                );
            }

            setState(LifecycleState.SHUTTING_DOWN);
        }

        try {
            logger.info("Shutting down DataManager");

            // Model doesn't have a close() or shutdown() method
            // So we just clean up our internal state

            // Optional: Perform any cleanup logic here
            synchronized (stateLock) {
                this.config = null;
                setState(LifecycleState.SHUTDOWN);
            }

            logger.info("DataManager shut down successfully. State: {}", state);

        } catch (Exception e) {
            logger.error("Shutdown failed", e);

            // Even on failure, move to SHUTDOWN state
            synchronized (stateLock) {
                this.config = null;
                setState(LifecycleState.SHUTDOWN);
            }

            throw new DataManagerException(
                    ErrorCode.SHUTDOWN_FAILED,
                    "Shutdown failed: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Returns the current lifecycle state.
     *
     * @return Current state
     */
    @Override
    public LifecycleState getState() {
        return state;
    }

    /**
     * Returns the currently used configuration.
     *
     * @return Current configuration, or null if not initialized
     */
    @Override
    public DataManagerConfig getConfig() {
        return config;
    }

    /**
     * Sets the lifecycle state.
     * This method should only be called within synchronized blocks.
     *
     * @param newState the new state to set
     */
    private void setState(LifecycleState newState) {
        logger.debug("State transition: {} -> {}", state, newState);
        this.state = newState;
    }

    /**
     * Checks if the DataManager can be used in the current state.
     *
     * @throws IllegalStateException if not in RUNNING state
     */
    public void checkUsable() {
        if (!state.isRunning()) {
            throw new IllegalStateException(
                    "DataManager is not usable in state: " + state +
                            ". Must be in RUNNING state."
            );
        }
    }

    @Override
    public String toString() {
        return "LifecycleManager{" +
                "state=" + state +
                ", configPresent=" + (config != null) +
                '}';
    }
}