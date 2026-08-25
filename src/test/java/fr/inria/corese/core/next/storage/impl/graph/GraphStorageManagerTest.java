package fr.inria.corese.core.next.storage.impl.graph;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.storage.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.transaction.TransactionManager;
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
            GraphStorageManager.Builder builder = GraphStorageManager.builder().valueFactory(mockFactory);
            assertThrows(IllegalStateException.class, builder::build);
        }

        @Test
        @DisplayName("Should throw when valueFactory is null")
        void shouldThrowWhenValueFactoryIsNull() {
            GraphStorageManager.Builder builder = GraphStorageManager.builder().graph(mockGraph);
            assertThrows(IllegalStateException.class, builder::build);
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
            assertNotNull(storageManager.queries());
            assertInstanceOf(GraphQueryOperations.class,
                    storageManager.queries());
        }

        @Test
        @DisplayName("Should return MutationOperations")
        void shouldReturnMutationOperations() {
            assertNotNull(storageManager.mutations());
            assertInstanceOf(GraphMutationOperations.class,
                    storageManager.mutations());
        }

        @Test
        @DisplayName("Should return MetadataOperations")
        void shouldReturnMetadataOperations() {
            assertNotNull(storageManager.metadata());
            assertInstanceOf(GraphMetadataOperations.class,
                    storageManager.metadata());
        }

        @Test
        @DisplayName("Should return TransactionManager")
        void shouldReturnTransactionManager() {
            assertNotNull(storageManager.transactions());
            assertInstanceOf(GraphTransactionManager.class,
                    storageManager.transactions());
        }

        @Test
        @DisplayName("Should return StorageLifecycle")
        void shouldReturnStorageLifecycle() {
            assertNotNull(storageManager.lifecycle());
            assertInstanceOf(GraphLifecycleManager.class,
                    storageManager.lifecycle());
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
                    storageManager.lifecycle().getState());
        }

        @Test
        @DisplayName("Should initialize successfully")
        void shouldInitializeSuccessfully() {
            StorageConfig config = StorageConfig.builder().build();

            storageManager.lifecycle().initialize(config);

            assertEquals(LifecycleState.RUNNING,
                    storageManager.lifecycle().getState());
            verify(mockGraph).init();
        }

        @Test
        @DisplayName("Should shutdown successfully")
        void shouldShutdownSuccessfully() {
            StorageConfig config = StorageConfig.builder().build();
            storageManager.lifecycle().initialize(config);

            storageManager.lifecycle().shutdown();

            assertEquals(LifecycleState.SHUTDOWN,
                    storageManager.lifecycle().getState());
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
            assertFalse(storageManager.transactions().supportsTransactions());
        }

        @Test
        @DisplayName("Should throw when beginning transaction")
        void shouldThrowWhenBeginningTransaction() {
            TransactionManager txManager = storageManager.transactions();
            assertThrows(UnsupportedOperationException.class, txManager::beginTransaction);
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
