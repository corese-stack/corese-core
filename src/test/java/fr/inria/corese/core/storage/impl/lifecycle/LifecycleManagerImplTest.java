package fr.inria.corese.core.storage.impl.lifecycle;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.storage.api.datamanager.lifecycle.LifecycleState;
import fr.inria.corese.core.storage.api.datamanager.support.config.DataManagerConfig;
import fr.inria.corese.core.storage.api.datamanager.support.exception.DataManagerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LifecycleManagerImpl.
 */
@DisplayName("LifecycleManagerImpl Tests")
class LifecycleManagerImplTest {


    private LifecycleManagerImpl lifecycleManager;

    @BeforeEach
    void setUp() {
        Graph graph = new Graph();
        lifecycleManager = new LifecycleManagerImpl(graph);
    }

    /**
     * Helper method to create a default test configuration.
     */
    private DataManagerConfig createDefaultConfig() {
        return DataManagerConfig.builder()
                .build();
    }

    /**
     * Helper method to create a test configuration with debug enabled.
     */
    private DataManagerConfig createDebugConfig() {
        return DataManagerConfig.builder()
                .debug(true)
                .build();
    }

    @Test
    @DisplayName("Initial state should be NOT_INITIALIZED")
    void testInitialStateIsNotInitialized() {
        assertEquals(LifecycleState.NOT_INITIALIZED, lifecycleManager.getState());
        assertFalse(lifecycleManager.isInitialized());
    }

    @Test
    @DisplayName("Initialize with valid config should succeed")
    void testInitializeSuccess() throws DataManagerException {
        DataManagerConfig config = createDebugConfig();
        lifecycleManager.initialize(config);

        assertEquals(LifecycleState.RUNNING, lifecycleManager.getState());
        assertTrue(lifecycleManager.isInitialized());
    }

    @Test
    @DisplayName("Initialize with null config should throw IllegalArgumentException")
    void testInitializeWithNullConfigThrows() {
        assertThrows(IllegalArgumentException.class, () -> lifecycleManager.initialize(null));
    }


    @Test
    @DisplayName("Shutdown after initialization should succeed")
    void testShutdownSuccess() throws DataManagerException {
        DataManagerConfig config = createDefaultConfig();
        lifecycleManager.initialize(config);

        lifecycleManager.shutdown();

        assertEquals(LifecycleState.SHUTDOWN, lifecycleManager.getState());
        assertFalse(lifecycleManager.isInitialized());
    }

    @Test
    @DisplayName("Restart should reinitialize successfully")
    void testRestartSuccess() throws DataManagerException {
        DataManagerConfig config1 = DataManagerConfig.builder()
                .debug(false)
                .build();
        lifecycleManager.initialize(config1);

        DataManagerConfig config2 = createDebugConfig();
        lifecycleManager.restart(config2);

        assertEquals(LifecycleState.RUNNING, lifecycleManager.getState());
        assertTrue(lifecycleManager.isInitialized());
    }

    @Test
    @DisplayName("checkUsable when RUNNING should not throw")
    void testCheckUsableWhenRunning() throws DataManagerException {
        DataManagerConfig config = createDefaultConfig();
        lifecycleManager.initialize(config);

        // Should not throw
        assertDoesNotThrow(() -> lifecycleManager.checkUsable());
    }

    @Test
    @DisplayName("checkUsable when NOT_INITIALIZED should throw IllegalStateException")
    void testCheckUsableWhenNotInitialized() {
        assertThrows(IllegalStateException.class, () -> lifecycleManager.checkUsable());
    }

    @Test
    @DisplayName("checkUsable after shutdown should throw IllegalStateException")
    void testCheckUsableAfterShutdown() throws DataManagerException {
        DataManagerConfig config = createDefaultConfig();
        lifecycleManager.initialize(config);
        lifecycleManager.shutdown();

        assertThrows(IllegalStateException.class, () -> lifecycleManager.checkUsable());
    }

    @Test
    @DisplayName("Initialize with custom storage path")
    void testInitializeWithCustomStoragePath() throws DataManagerException {
        String customPath = "http://example.org/custom/storage";
        DataManagerConfig config = DataManagerConfig.builder()
                .build();

        lifecycleManager.initialize(config);

        assertTrue(lifecycleManager.isInitialized());
        assertEquals(LifecycleState.RUNNING, lifecycleManager.getState());
    }

    @Test
    @DisplayName("Initialize with transaction support enabled")
    void testInitializeWithTransactionSupport() throws DataManagerException {
        DataManagerConfig config = DataManagerConfig.builder()
                .transactionSupport(true)
                .build();

        lifecycleManager.initialize(config);

        assertTrue(lifecycleManager.isInitialized());
    }

    @Test
    @DisplayName("Initialize with custom properties")
    void testInitializeWithCustomProperties() throws DataManagerException {
        DataManagerConfig config = DataManagerConfig.builder()
                .property("custom.key", "custom.value")
                .property("another.key", "another.value")
                .build();

        lifecycleManager.initialize(config);

        assertTrue(lifecycleManager.isInitialized());
        assertEquals(LifecycleState.RUNNING, lifecycleManager.getState());
    }

    @Test
    @DisplayName("Multiple restarts should work correctly")
    void testMultipleRestarts() throws DataManagerException {
        DataManagerConfig config1 = DataManagerConfig.builder()
                .debug(false)
                .build();
        lifecycleManager.initialize(config1);

        for (int i = 0; i < 3; i++) {
            DataManagerConfig config = DataManagerConfig.builder()
                    .debug(i % 2 == 0)
                    .build();
            lifecycleManager.restart(config);

            assertEquals(LifecycleState.RUNNING, lifecycleManager.getState());
            assertTrue(lifecycleManager.isInitialized());
        }
    }

    @Test
    @DisplayName("State transitions should be correct")
    void testStateTransitions() throws DataManagerException {
        assertEquals(LifecycleState.NOT_INITIALIZED, lifecycleManager.getState());

        DataManagerConfig config = createDefaultConfig();
        lifecycleManager.initialize(config);
        assertEquals(LifecycleState.RUNNING, lifecycleManager.getState());

        lifecycleManager.shutdown();
        assertEquals(LifecycleState.SHUTDOWN, lifecycleManager.getState());
    }
}