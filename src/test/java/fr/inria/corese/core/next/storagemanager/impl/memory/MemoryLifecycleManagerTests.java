package fr.inria.corese.core.next.storagemanager.impl.memory;

import fr.inria.corese.core.next.storagemanager.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.storagemanager.api.support.config.StorageConfig;
import fr.inria.corese.core.next.storagemanager.api.support.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MemoryLifecycleManager tests")
class MemoryLifecycleManagerTests {

    private InMemoryStatementStore adapter;
    private MemoryLifecycleManager lifecycleManager;

    @BeforeEach
    void setUp() {
        adapter = new InMemoryStatementStore();
        lifecycleManager = new MemoryLifecycleManager(adapter);
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw when adapter is null")
        void shouldThrowWhenAdapterIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> new MemoryLifecycleManager(null));
        }

        @Test
        @DisplayName("Should start in NOT_INITIALIZED state")
        void shouldStartInNotInitializedState() {
            assertEquals(LifecycleState.NOT_INITIALIZED,
                    lifecycleManager.getState());
        }
    }

    @Nested
    @DisplayName("initialize() tests")
    class InitializeTests {

        @Test
        @DisplayName("Should throw when config is null")
        void shouldThrowWhenConfigIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> lifecycleManager.initialize(null));
        }

        @Test
        @DisplayName("Should initialize successfully")
        void shouldInitializeSuccessfully() throws StorageException {
            StorageConfig config = StorageConfig.builder().build();

            lifecycleManager.initialize(config);

            assertEquals(LifecycleState.RUNNING, lifecycleManager.getState());
            assertEquals(config, lifecycleManager.getConfig());
        }

        @Test
        @DisplayName("Should throw when already initialized")
        void shouldThrowWhenAlreadyInitialized() throws StorageException {
            StorageConfig config = StorageConfig.builder().build();
            lifecycleManager.initialize(config);

            assertThrows(IllegalStateException.class,
                    () -> lifecycleManager.initialize(config));
        }
    }

    @Nested
    @DisplayName("shutdown() tests")
    class ShutdownTests {

        @Test
        @DisplayName("Should throw when not running")
        void shouldThrowWhenNotRunning() {
            assertThrows(IllegalStateException.class,
                    () -> lifecycleManager.shutdown());
        }

        @Test
        @DisplayName("Should shutdown successfully")
        void shouldShutdownSuccessfully() throws StorageException {
            StorageConfig config = StorageConfig.builder().build();
            lifecycleManager.initialize(config);

            lifecycleManager.shutdown();

            assertEquals(LifecycleState.SHUTDOWN, lifecycleManager.getState());
            assertNull(lifecycleManager.getConfig());
        }

        @Test
        @DisplayName("Should not clear data on shutdown")
        void shouldNotClearDataOnShutdown() throws StorageException {
            // Add some data
            adapter.add(org.mockito.Mockito.mock(fr.inria.corese.core.next.data.api.Statement.class));
            int sizeBefore = adapter.size();

            // Initialize and shutdown
            StorageConfig config = StorageConfig.builder().build();
            lifecycleManager.initialize(config);
            lifecycleManager.shutdown();

            // Data should still be there
            assertEquals(sizeBefore, adapter.size());
        }
    }
}

