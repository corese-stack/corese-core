package fr.inria.corese.core.next.storage.api.operations;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.PredicateStatistics;
import fr.inria.corese.core.next.storage.api.model.StorageStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("MetadataOperations Default Methods Tests")
class MetadataOperationsTest {

    private static class StubMetadataOperations implements MetadataOperations {
        private PredicateStatistics returnedStats;

        void setReturnedStats(PredicateStatistics stats) {
            this.returnedStats = stats;
        }

        @Override
        public Set<IRI> getPredicates() throws StorageException {
            return Set.of();
        }

        @Override
        public Set<Resource> getSubjects() throws StorageException {
            return Set.of();
        }

        @Override
        public Set<Value> getObjects() throws StorageException {
            return Set.of();
        }

        @Override
        public Set<Resource> getContexts() throws StorageException {
            return Set.of();
        }

        @Override
        public StorageStatistics getStatistics() throws StorageException {
            return null;
        }

        @Override
        public PredicateStatistics getPredicateStatistics(IRI predicate) throws StorageException {
            return returnedStats;
        }
    }

    @Test
    @DisplayName("findPredicateStatistics should return empty optional when getPredicateStatistics is null")
    void testFindPredicateStatisticsEmpty() throws StorageException {
        StubMetadataOperations operations = new StubMetadataOperations();
        IRI predicate = mock(IRI.class);

        Optional<PredicateStatistics> stats = operations.findPredicateStatistics(predicate);

        assertNotNull(stats);
        assertTrue(stats.isEmpty());
    }

    @Test
    @DisplayName("findPredicateStatistics should return present optional when getPredicateStatistics returns stats")
    void testFindPredicateStatisticsPresent() throws StorageException {
        StubMetadataOperations operations = new StubMetadataOperations();
        IRI predicate = mock(IRI.class);
        PredicateStatistics expected = new PredicateStatistics(predicate, 42L, 10L, 20L);
        operations.setReturnedStats(expected);

        Optional<PredicateStatistics> stats = operations.findPredicateStatistics(predicate);

        assertNotNull(stats);
        assertTrue(stats.isPresent());
        assertEquals(expected, stats.get());
    }

    @Test
    @DisplayName("Default implementation of getPredicateStatistics returns null")
    void testDefaultGetPredicateStatisticsReturnsNull() throws StorageException {
        MetadataOperations defaultOps = new MetadataOperations() {
            @Override public Set<IRI> getPredicates() { return Set.of(); }
            @Override public Set<Resource> getSubjects() { return Set.of(); }
            @Override public Set<Value> getObjects() { return Set.of(); }
            @Override public Set<Resource> getContexts() { return Set.of(); }
            @Override public StorageStatistics getStatistics() { return null; }
        };

        IRI predicate = mock(IRI.class);
        assertNull(defaultOps.getPredicateStatistics(predicate));
        assertTrue(defaultOps.findPredicateStatistics(predicate).isEmpty());
    }
}
