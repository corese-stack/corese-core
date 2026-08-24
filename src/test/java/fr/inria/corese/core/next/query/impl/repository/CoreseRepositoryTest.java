package fr.inria.corese.core.next.query.impl.repository;

import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.repository.RepositoryConnection;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoreseRepositoryTest {

    private CoreseRepository repository;

    @BeforeEach
    void setUp() {
        MemoryStorageManager storage = MemoryStorageManager.builder().build();
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
        @DisplayName("init() twice throws RepositoryException")
        void initTwiceThrows() throws RepositoryException {
            repository.init();
            assertThrows(RepositoryException.class, repository::init);
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

    @Nested
    @DisplayName("Convenience 1-liner Shortcuts (Level 1)")
    class ConvenienceShortcuts {

        @Test
        @DisplayName("select() 1-liner executes SELECT query and returns materialized safe result")
        void selectOneLinerTest() throws RepositoryException {
            repository.init();
            repository.update("INSERT DATA { <http://ex.org/s> <http://ex.org/p> <http://ex.org/o> }");

            var result = repository.select("SELECT * WHERE { ?s ?p ?o }");
            try (result) {
                assertTrue(result.hasNext());
                var bs = result.next();
                assertEquals("http://ex.org/s", bs.getValue("s").stringValue());
            }
            assertThrows(IllegalStateException.class, result::hasNext);
        }

        @Test
        @DisplayName("ask() 1-liner executes ASK query")
        void askOneLinerTest() throws RepositoryException {
            repository.init();
            repository.update("INSERT DATA { <http://ex.org/s> <http://ex.org/p> <http://ex.org/o> }");

            assertTrue(repository.ask("ASK WHERE { <http://ex.org/s> ?p ?o }"));
            assertFalse(repository.ask("ASK WHERE { <http://ex.org/nobody> ?p ?o }"));
        }

        @Test
        @DisplayName("construct() 1-liner executes CONSTRUCT query and returns materialized safe result")
        void constructOneLinerTest() throws RepositoryException {
            repository.init();
            repository.update("INSERT DATA { <http://ex.org/s> <http://ex.org/p> <http://ex.org/o> }");

            var result = repository.construct("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
            try (result) {
                assertTrue(result.hasNext());
                var stmt = result.next();
                assertEquals("http://ex.org/s", stmt.getSubject().stringValue());
            }
            assertThrows(IllegalStateException.class, result::hasNext);
        }
    }
}
