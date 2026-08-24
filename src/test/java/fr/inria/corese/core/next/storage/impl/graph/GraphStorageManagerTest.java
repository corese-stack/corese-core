package fr.inria.corese.core.next.storage.impl.graph;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.storage.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GraphStorageManager.
 */
class GraphStorageManagerTest {

    @Mock
    private Graph mockGraph;

    @Mock
    private ValueFactory mockFactory;

    private GraphStorageManager storageManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("Builder tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build with graph and valueFactory")
        void shouldBuildWithGraphAndValueFactory() {
            storageManager = GraphStorageManager.builder()
                    .graph(mockGraph)
                    .valueFactory(mockFactory)
                    .build();

            assertNotNull(storageManager);
            assertEquals(mockGraph, storageManager.getGraph());
        }

        @Test
        @DisplayName("Should throw when graph is null")
        void shouldThrowWhenGraphIsNull() {
            assertThrows(IllegalStateException.class, () -> GraphStorageManager.builder()
                    .valueFactory(mockFactory)
                    .build());
        }

        @Test
        @DisplayName("Should throw when valueFactory is null")
        void shouldThrowWhenValueFactoryIsNull() {
            assertThrows(IllegalStateException.class, () -> GraphStorageManager.builder()
                    .graph(mockGraph)
                    .build());
        }
    }

    @Nested
    @DisplayName("Operations getters")
    class OperationsGettersTests {

        @BeforeEach
        void buildStorageManager() {
            storageManager = GraphStorageManager.builder()
                    .graph(mockGraph)
                    .valueFactory(mockFactory)
                    .build();
        }

        @Test
        @DisplayName("Should return QueryOperations")
        void shouldReturnQueryOperations() {
            assertNotNull(storageManager.getQueryOperations());
            assertInstanceOf(GraphQueryOperations.class,
                    storageManager.getQueryOperations());
        }

        @Test
        @DisplayName("Should return MutationOperations")
        void shouldReturnMutationOperations() {
            assertNotNull(storageManager.getMutationOperations());
            assertInstanceOf(GraphMutationOperations.class,
                    storageManager.getMutationOperations());
        }

        @Test
        @DisplayName("Should return MetadataOperations")
        void shouldReturnMetadataOperations() {
            assertNotNull(storageManager.getMetadataOperations());
            assertInstanceOf(GraphMetadataOperations.class,
                    storageManager.getMetadataOperations());
        }

        @Test
        @DisplayName("Should return BulkOperations")
        void shouldReturnBulkOperations() {
            assertNotNull(storageManager.getBulkOperations());
            assertInstanceOf(GraphBulkOperations.class,
                    storageManager.getBulkOperations());
        }

        @Test
        @DisplayName("Should return TransactionManager")
        void shouldReturnTransactionManager() {
            assertNotNull(storageManager.getTransactionManager());
            assertInstanceOf(GraphTransactionManager.class,
                    storageManager.getTransactionManager());
        }

        @Test
        @DisplayName("Should return StorageLifecycle")
        void shouldReturnStorageLifecycle() {
            assertNotNull(storageManager.getLifecycle());
            assertInstanceOf(GraphLifecycleManager.class,
                    storageManager.getLifecycle());
        }
    }

    @Nested
    @DisplayName("Lifecycle tests")
    class LifecycleTests {

        @BeforeEach
        void buildStorageManager() {
            storageManager = GraphStorageManager.builder()
                    .graph(mockGraph)
                    .valueFactory(mockFactory)
                    .build();
        }

        @Test
        @DisplayName("Should start in NOT_INITIALIZED state")
        void shouldStartInNotInitializedState() {
            assertEquals(LifecycleState.NOT_INITIALIZED,
                    storageManager.getLifecycle().getState());
        }

        @Test
        @DisplayName("Should initialize successfully")
        void shouldInitializeSuccessfully() {
            StorageConfig config = StorageConfig.builder().build();

            storageManager.getLifecycle().initialize(config);

            assertEquals(LifecycleState.RUNNING,
                    storageManager.getLifecycle().getState());
            verify(mockGraph).init();
        }

        @Test
        @DisplayName("Should shutdown successfully")
        void shouldShutdownSuccessfully() {
            StorageConfig config = StorageConfig.builder().build();
            storageManager.getLifecycle().initialize(config);

            storageManager.getLifecycle().shutdown();

            assertEquals(LifecycleState.SHUTDOWN,
                    storageManager.getLifecycle().getState());
        }
    }

    @Nested
    @DisplayName("Transaction tests")
    class TransactionTests {

        @BeforeEach
        void buildStorageManager() {
            storageManager = GraphStorageManager.builder()
                    .graph(mockGraph)
                    .valueFactory(mockFactory)
                    .build();
        }

        @Test
        @DisplayName("Should not support transactions")
        void shouldNotSupportTransactions() {
            assertFalse(storageManager.getTransactionManager().supportsTransactions());
        }

        @Test
        @DisplayName("Should throw when beginning transaction")
        void shouldThrowWhenBeginningTransaction() {
            assertThrows(UnsupportedOperationException.class, () -> storageManager.getTransactionManager().beginTransaction());
        }
    }

    @Nested
    @DisplayName("toString tests")
    class ToStringTests {

        @Test
        @DisplayName("Should return meaningful toString")
        void shouldReturnMeaningfulToString() {
            when(mockGraph.size()).thenReturn(42);
            storageManager = GraphStorageManager.builder()
                    .graph(mockGraph)
                    .valueFactory(mockFactory)
                    .build();

            String result = storageManager.toString();

            assertNotNull(result);
            assertTrue(result.contains("GraphStorageManager"));
            assertTrue(result.contains("42"));
        }
    }
}
