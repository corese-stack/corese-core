package fr.inria.corese.core.next.datamanager.impl.lifecycle;

import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.datamanager.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.datamanager.api.support.config.DataManagerConfig;
import fr.inria.corese.core.next.datamanager.api.support.exception.DataManagerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LifecycleManagerImpl.
 */
@DisplayName("LifecycleManagerImpl Tests")
class LifecycleManagerImplTest {

    @Mock
    private Model model;

    private LifecycleManagerImpl lifecycle;
    private DataManagerConfig config;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lifecycle = new LifecycleManagerImpl(model);
        config = DataManagerConfig.builder()
                .debug(true)
                .build();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when constructed with null model")
    void testConstructorNullModel() {
        assertThrows(IllegalArgumentException.class,
                () -> new LifecycleManagerImpl(null));
    }

    @Test
    @DisplayName("Should start in NOT_INITIALIZED state with no config")
    void testInitialState() {
        assertEquals(LifecycleState.NOT_INITIALIZED, lifecycle.getState());
        assertNull(lifecycle.getConfig());
        assertFalse(lifecycle.isInitialized());
    }

    @Test
    @DisplayName("Should successfully initialize and transition to RUNNING state")
    void testInitializeSuccess() throws DataManagerException {
        lifecycle.initialize(config);

        assertEquals(LifecycleState.RUNNING, lifecycle.getState());
        assertEquals(config, lifecycle.getConfig());
        assertTrue(lifecycle.isInitialized());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when initialized with null config")
    void testInitializeNullConfig() {
        assertThrows(IllegalArgumentException.class,
                () -> lifecycle.initialize(null));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when attempting to initialize twice")
    void testInitializeWhenAlreadyInitialized() throws DataManagerException {
        lifecycle.initialize(config);

        assertThrows(IllegalStateException.class,
                () -> lifecycle.initialize(config));
    }

    @Test
    @DisplayName("Should successfully shutdown and transition to SHUTDOWN state")
    void testShutdownSuccess() throws DataManagerException {
        lifecycle.initialize(config);
        lifecycle.shutdown();

        assertEquals(LifecycleState.SHUTDOWN, lifecycle.getState());
        assertNull(lifecycle.getConfig());
        assertFalse(lifecycle.isInitialized());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when shutting down without initialization")
    void testShutdownWhenNotInitialized() {
        assertThrows(IllegalStateException.class,
                () -> lifecycle.shutdown());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when attempting to shutdown twice")
    void testShutdownWhenAlreadyShutdown() throws DataManagerException {
        lifecycle.initialize(config);
        lifecycle.shutdown();

        assertThrows(IllegalStateException.class,
                () -> lifecycle.shutdown());
    }

    @Test
    @DisplayName("Should allow restart without previous initialization")
    void testRestartWithoutPreviousInit() throws DataManagerException {
        DataManagerConfig newConfig = DataManagerConfig.builder()
                .debug(false)
                .build();

        lifecycle.restart(newConfig);

        assertEquals(LifecycleState.RUNNING, lifecycle.getState());
        assertEquals(newConfig, lifecycle.getConfig());
    }

    @Test
    @DisplayName("Should not throw when checkUsable() is called in RUNNING state")
    void testCheckUsableWhenRunning() throws DataManagerException {
        lifecycle.initialize(config);

        assertDoesNotThrow(() -> lifecycle.checkUsable());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when checkUsable() called before initialization")
    void testCheckUsableWhenNotRunning() {
        assertThrows(IllegalStateException.class,
                () -> lifecycle.checkUsable());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when checkUsable() called after shutdown")
    void testCheckUsableAfterShutdown() throws DataManagerException {
        lifecycle.initialize(config);
        lifecycle.shutdown();

        assertThrows(IllegalStateException.class,
                () -> lifecycle.checkUsable());
    }

    @Test
    @DisplayName("Should transition through states correctly: NOT_INITIALIZED -> RUNNING -> SHUTDOWN")
    void testStateTransitions() throws DataManagerException {
        assertEquals(LifecycleState.NOT_INITIALIZED, lifecycle.getState());

        lifecycle.initialize(config);
        assertEquals(LifecycleState.RUNNING, lifecycle.getState());

        lifecycle.shutdown();
        assertEquals(LifecycleState.SHUTDOWN, lifecycle.getState());
    }

    @Test
    @DisplayName("Should include state and config presence in toString() output")
    void testToString() throws DataManagerException {
        String beforeInit = lifecycle.toString();
        assertTrue(beforeInit.contains("LifecycleManager"));
        assertTrue(beforeInit.contains("NOT_INITIALIZED"));
        assertTrue(beforeInit.contains("configPresent=false"));

        lifecycle.initialize(config);

        String afterInit = lifecycle.toString();
        assertTrue(afterInit.contains("RUNNING"));
        assertTrue(afterInit.contains("configPresent=true"));
    }

    @Test
    @DisplayName("Should clear config reference on shutdown while preserving original config instance")
    void testConfigImmutabilityAfterShutdown() throws DataManagerException {
        lifecycle.initialize(config);
        DataManagerConfig originalConfig = lifecycle.getConfig();

        lifecycle.shutdown();

        assertNull(lifecycle.getConfig());
        assertNotNull(originalConfig);
    }
}