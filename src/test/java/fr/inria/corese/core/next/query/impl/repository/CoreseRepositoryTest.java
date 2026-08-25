package fr.inria.corese.core.next.query.impl.repository;

import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.repository.RepositoryConnection;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.AfterEach;
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

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("Open immediately after construction")
        void openAfterConstruction() {
            assertTrue(repository.isOpen());
        }

        @Test
        @DisplayName("Closed after close()")
        void closedAfterClose() {
            repository.close();
            assertFalse(repository.isOpen());
        }

        @Test
        @DisplayName("close() is idempotent")
        void closeIsIdempotent() {
            repository.close();
            assertDoesNotThrow(repository::close);
        }

        @Test
        @DisplayName("getConnection() after close() throws RepositoryException")
        void getConnectionAfterCloseThrows() {
            repository.close();
            assertThrows(RepositoryException.class, repository::getConnection);
        }

        @Test
        @DisplayName("getConnection() returns open connection")
        void getConnectionReturnsOpenConnection() {
            try (RepositoryConnection conn = repository.getConnection()) {
                assertTrue(conn.isOpen());
                assertSame(repository, conn.getRepository());
            }
        }

        @Test
        @DisplayName("Closing repository invalidates existing connections")
        void closeInvalidatesConnections() {
            RepositoryConnection connection = repository.getConnection();
            repository.close();

            assertFalse(connection.isOpen());
            assertThrows(RepositoryException.class, () -> connection.setDataset(null));
            connection.close();
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
            repository.update("INSERT DATA { <http://ex.org/s> <http://ex.org/p> <http://ex.org/o> }");

            assertTrue(repository.ask("ASK WHERE { <http://ex.org/s> ?p ?o }"));
            assertFalse(repository.ask("ASK WHERE { <http://ex.org/nobody> ?p ?o }"));
        }

        @Test
        @DisplayName("graph() 1-liner executes CONSTRUCT query and returns materialized safe result")
        void constructOneLinerTest() throws RepositoryException {
            repository.update("INSERT DATA { <http://ex.org/s> <http://ex.org/p> <http://ex.org/o> }");

            var result = repository.graph("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
            try (result) {
                assertTrue(result.hasNext());
                var stmt = result.next();
                assertEquals("http://ex.org/s", stmt.getSubject().stringValue());
            }
            assertThrows(IllegalStateException.class, result::hasNext);
        }
    }
}
