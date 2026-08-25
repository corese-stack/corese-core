package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.storage.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.transaction.TransactionManager;
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
            assertNotNull(storage.queries());
            assertNotNull(storage.mutations());
            assertNotNull(storage.metadata());
            assertNotNull(storage.transactions());
            assertNotNull(storage.lifecycle());
        }
    }

    @Nested
    @DisplayName("Operations getters")
    class OperationsGettersTests {

        @Test
        @DisplayName("Should return QueryOperations")
        void shouldReturnQueryOperations() {
            assertNotNull(storageManager.queries());
            assertInstanceOf(MemoryQueryOperations.class,
                    storageManager.queries());
        }

        @Test
        @DisplayName("Should return MutationOperations")
        void shouldReturnMutationOperations() {
            assertNotNull(storageManager.mutations());
            assertInstanceOf(MemoryMutationOperations.class,
                    storageManager.mutations());
        }

        @Test
        @DisplayName("Should return MetadataOperations")
        void shouldReturnMetadataOperations() {
            assertNotNull(storageManager.metadata());
            assertInstanceOf(MemoryMetadataOperations.class,
                    storageManager.metadata());
        }

        @Test
        @DisplayName("Should return TransactionManager")
        void shouldReturnTransactionManager() {
            assertNotNull(storageManager.transactions());
            assertInstanceOf(MemoryTransactionManager.class,
                    storageManager.transactions());
        }

        @Test
        @DisplayName("Should return StorageLifecycle")
        void shouldReturnStorageLifecycle() {
            assertNotNull(storageManager.lifecycle());
            assertInstanceOf(MemoryLifecycleManager.class,
                    storageManager.lifecycle());
        }
    }

    @Nested
    @DisplayName("Lifecycle tests")
    class LifecycleTests {

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
            assertEquals(java.util.Optional.of(config), storageManager.lifecycle().getConfig());
        }

        @Test
        @DisplayName("Should shutdown successfully")
        void shouldShutdownSuccessfully() {
            StorageConfig config = StorageConfig.builder().build();
            storageManager.lifecycle().initialize(config);

            storageManager.lifecycle().shutdown();

            assertEquals(LifecycleState.SHUTDOWN,
                storageManager.lifecycle().getState());
            assertTrue(storageManager.lifecycle().getConfig().isEmpty());
        }
    }

    @Nested
    @DisplayName("Transaction tests")
    class TransactionTests {

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
            String result = storageManager.toString();

            assertNotNull(result);
            assertTrue(result.contains("MemoryStorageManager"));
            assertTrue(result.contains("size="));
        }
    }
}
