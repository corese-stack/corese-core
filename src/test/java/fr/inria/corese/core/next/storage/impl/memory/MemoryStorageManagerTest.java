package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.storage.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemoryStorageManager.
 */
class MemoryStorageManagerTest {

    private MemoryStorageManager storageManager;

    @BeforeEach
    void setUp() {
        storageManager = MemoryStorageManager.builder().build();
    }

    @Nested
    @DisplayName("Builder tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build with default settings")
        void shouldBuildWithDefaultSettings() {
            MemoryStorageManager storage = MemoryStorageManager.builder().build();

            assertNotNull(storage);
            assertNotNull(storage.getQueryOperations());
            assertNotNull(storage.getMutationOperations());
            assertNotNull(storage.getMetadataOperations());
            assertNotNull(storage.getBulkOperations());
            assertNotNull(storage.getTransactionManager());
            assertNotNull(storage.getLifecycle());
        }
    }

    @Nested
    @DisplayName("Operations getters")
    class OperationsGettersTests {

        @Test
        @DisplayName("Should return QueryOperations")
        void shouldReturnQueryOperations() {
            assertNotNull(storageManager.getQueryOperations());
            assertInstanceOf(MemoryQueryOperations.class,
                    storageManager.getQueryOperations());
        }

        @Test
        @DisplayName("Should return MutationOperations")
        void shouldReturnMutationOperations() {
            assertNotNull(storageManager.getMutationOperations());
            assertInstanceOf(MemoryMutationOperations.class,
                    storageManager.getMutationOperations());
        }

        @Test
        @DisplayName("Should return MetadataOperations")
        void shouldReturnMetadataOperations() {
            assertNotNull(storageManager.getMetadataOperations());
            assertInstanceOf(MemoryMetadataOperations.class,
                    storageManager.getMetadataOperations());
        }

        @Test
        @DisplayName("Should return BulkOperations")
        void shouldReturnBulkOperations() {
            assertNotNull(storageManager.getBulkOperations());
            assertInstanceOf(MemoryBulkOperations.class,
                    storageManager.getBulkOperations());
        }

        @Test
        @DisplayName("Should return TransactionManager")
        void shouldReturnTransactionManager() {
            assertNotNull(storageManager.getTransactionManager());
            assertInstanceOf(MemoryTransactionManager.class,
                    storageManager.getTransactionManager());
        }

        @Test
        @DisplayName("Should return StorageLifecycle")
        void shouldReturnStorageLifecycle() {
            assertNotNull(storageManager.getLifecycle());
            assertInstanceOf(MemoryLifecycleManager.class,
                    storageManager.getLifecycle());
        }
    }

    @Nested
    @DisplayName("Lifecycle tests")
    class LifecycleTests {

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
            assertEquals(java.util.Optional.of(config), storageManager.getLifecycle().getConfig());
        }

        @Test
        @DisplayName("Should shutdown successfully")
        void shouldShutdownSuccessfully() {
            StorageConfig config = StorageConfig.builder().build();
            storageManager.getLifecycle().initialize(config);

            storageManager.getLifecycle().shutdown();

            assertEquals(LifecycleState.SHUTDOWN,
                storageManager.getLifecycle().getState());
            assertTrue(storageManager.getLifecycle().getConfig().isEmpty());
        }
    }

    @Nested
    @DisplayName("Transaction tests")
    class TransactionTests {

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
            String result = storageManager.toString();

            assertNotNull(result);
            assertTrue(result.contains("MemoryStorageManager"));
            assertTrue(result.contains("size="));
        }
    }
}
