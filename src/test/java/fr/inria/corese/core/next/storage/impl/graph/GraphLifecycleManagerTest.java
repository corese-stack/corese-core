package fr.inria.corese.core.next.storage.impl.graph;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.next.storage.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@DisplayName("GraphLifecycleManager tests")
class GraphLifecycleManagerTest {

    @Mock
    private Graph mockGraph;

    private GraphLifecycleManager lifecycleManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lifecycleManager = new GraphLifecycleManager(mockGraph);
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw when graph is null")
        void shouldThrowWhenGraphIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> new GraphLifecycleManager(null));
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
            assertEquals(java.util.Optional.of(config), lifecycleManager.getConfig());
            verify(mockGraph).init();
        }

        @Test
        @DisplayName("Should throw when already initialized")
        void shouldThrowWhenAlreadyInitialized() throws StorageException {
            StorageConfig config = StorageConfig.builder().build();
            lifecycleManager.initialize(config);

            assertThrows(IllegalStateException.class,
                    () -> lifecycleManager.initialize(config));
        }

        @Test
        @DisplayName("Should remain NOT_INITIALIZED on error")
        void shouldRemainNotInitializedOnError() {
            StorageConfig config = StorageConfig.builder().build();
            doThrow(new RuntimeException("Init failed")).when(mockGraph).init();

            assertThrows(StorageException.class,
                    () -> lifecycleManager.initialize(config));
            assertEquals(LifecycleState.NOT_INITIALIZED,
                    lifecycleManager.getState());
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
            assertTrue(lifecycleManager.getConfig().isEmpty());
        }
    }
}
