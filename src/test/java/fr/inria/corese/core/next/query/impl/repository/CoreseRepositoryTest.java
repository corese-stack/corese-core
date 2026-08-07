package fr.inria.corese.core.next.query.impl.repository;

import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.repository.RepositoryConnection;
import fr.inria.corese.core.next.storagemanager.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoreseRepositoryTest {

    private MemoryStorageManager storage;
    private CoreseRepository repository;

    @BeforeEach
    void setUp() {
        storage = MemoryStorageManager.builder().build();
        repository = new CoreseRepository(storage);
    }

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("Not initialized before init()")
        void notInitializedBeforeInit() {
            assertFalse(repository.isInitialized());
        }

        @Test
        @DisplayName("Initialized after init()")
        void initializedAfterInit() throws RepositoryException {
            repository.init();
            assertTrue(repository.isInitialized());
        }

        @Test
        @DisplayName("init() twice throws IllegalStateException")
        void initTwiceThrows() throws RepositoryException {
            repository.init();
            assertThrows(IllegalStateException.class, repository::init);
        }

        @Test
        @DisplayName("getConnection() before init() throws RepositoryException")
        void getConnectionBeforeInitThrows() {
            assertThrows(RepositoryException.class, repository::getConnection);
        }

        @Test
        @DisplayName("getConnection() after init() returns open connection")
        void getConnectionAfterInit() throws RepositoryException {
            repository.init();
            try (RepositoryConnection conn = repository.getConnection()) {
                assertTrue(conn.isOpen());
                assertSame(repository, conn.getRepository());
            }
        }

        @Test
        @DisplayName("getValueFactory() is not null")
        void valueFactoryNotNull() {
            assertNotNull(repository.getValueFactory());
        }
    }
}
