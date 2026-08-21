package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.StorageStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MemoryMetadataOperations.
 */
class MemoryMetadataOperationsTest {

    @Mock
    private InMemoryStatementStore mockAdapter;

    private MemoryMetadataOperations metadataOps;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        metadataOps = new MemoryMetadataOperations(mockAdapter);
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw when adapter is null")
        void shouldThrowWhenAdapterIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> new MemoryMetadataOperations(null));
        }
    }

    @Nested
    @DisplayName("getPredicates() tests")
    class GetPredicatesTests {

        @Test
        @DisplayName("Should return unmodifiable set of predicates")
        void shouldReturnUnmodifiableSetOfPredicates() throws StorageException {
            IRI predicate1 = mock(IRI.class);
            IRI predicate2 = mock(IRI.class);
            Set<IRI> mockPredicates = new HashSet<>(Arrays.asList(predicate1, predicate2));

            when(mockAdapter.getPredicates()).thenReturn(mockPredicates);

            Set<IRI> result = metadataOps.getPredicates();

            assertEquals(2, result.size());
            assertTrue(result.contains(predicate1));
            assertTrue(result.contains(predicate2));
            verify(mockAdapter).getPredicates();
        }

        @Test
        @DisplayName("Should return unmodifiable set")
        void shouldReturnUnmodifiableSet() throws StorageException {
            when(mockAdapter.getPredicates()).thenReturn(new HashSet<>());

            Set<IRI> result = metadataOps.getPredicates();

            assertThrows(UnsupportedOperationException.class,
                    () -> result.add(mock(IRI.class)));
        }

        @Test
        @DisplayName("Should return empty set when no predicates")
        void shouldReturnEmptySetWhenNoPredicates() throws StorageException {
            when(mockAdapter.getPredicates()).thenReturn(Collections.emptySet());

            Set<IRI> result = metadataOps.getPredicates();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getSubjects() tests")
    class GetSubjectsTests {

        @Test
        @DisplayName("Should return unmodifiable set of subjects")
        void shouldReturnUnmodifiableSetOfSubjects() {
            Resource subject1 = mock(Resource.class);
            Resource subject2 = mock(Resource.class);
            Set<Resource> mockSubjects = new HashSet<>(Arrays.asList(subject1, subject2));

            when(mockAdapter.getSubjects()).thenReturn(mockSubjects);

            Set<Resource> result = metadataOps.getSubjects();

            assertEquals(2, result.size());
            assertTrue(result.contains(subject1));
            assertTrue(result.contains(subject2));
            verify(mockAdapter).getSubjects();
        }

        @Test
        @DisplayName("Should return unmodifiable set")
        void shouldReturnUnmodifiableSet() {
            when(mockAdapter.getSubjects()).thenReturn(new HashSet<>());

            Set<Resource> result = metadataOps.getSubjects();

            assertThrows(UnsupportedOperationException.class,
                    () -> result.add(mock(Resource.class)));
        }
    }

    @Nested
    @DisplayName("getObjects() tests")
    class GetObjectsTests {

        @Test
        @DisplayName("Should return unmodifiable set of objects")
        void shouldReturnUnmodifiableSetOfObjects() {
            Value object1 = mock(Value.class);
            Value object2 = mock(Value.class);
            Value object3 = mock(Value.class);
            Set<Value> mockObjects = new HashSet<>(Arrays.asList(object1, object2, object3));

            when(mockAdapter.getObjects()).thenReturn(mockObjects);

            Set<Value> result = metadataOps.getObjects();

            assertEquals(3, result.size());
            assertTrue(result.contains(object1));
            assertTrue(result.contains(object2));
            assertTrue(result.contains(object3));
            verify(mockAdapter).getObjects();
        }

        @Test
        @DisplayName("Should return unmodifiable set")
        void shouldReturnUnmodifiableSet() {
            when(mockAdapter.getObjects()).thenReturn(new HashSet<>());

            Set<Value> result = metadataOps.getObjects();

            assertThrows(UnsupportedOperationException.class,
                    () -> result.add(mock(Value.class)));
        }
    }

    @Nested
    @DisplayName("getContexts() tests")
    class GetContextsTests {

        @Test
        @DisplayName("Should return unmodifiable set of contexts")
        void shouldReturnUnmodifiableSetOfContexts() throws StorageException {
            Resource context1 = mock(Resource.class);
            Resource context2 = mock(Resource.class);
            Set<Resource> mockContexts = new HashSet<>(Arrays.asList(context1, context2));

            when(mockAdapter.getContexts()).thenReturn(mockContexts);

            Set<Resource> result = metadataOps.getContexts();

            assertEquals(2, result.size());
            assertTrue(result.contains(context1));
            assertTrue(result.contains(context2));
            verify(mockAdapter).getContexts();
        }

        @Test
        @DisplayName("Should return unmodifiable set")
        void shouldReturnUnmodifiableSet() throws StorageException {
            when(mockAdapter.getContexts()).thenReturn(new HashSet<>());

            Set<Resource> result = metadataOps.getContexts();

            assertThrows(UnsupportedOperationException.class,
                    () -> result.add(mock(Resource.class)));
        }

        @Test
        @DisplayName("Should return empty set when no contexts")
        void shouldReturnEmptySetWhenNoContexts() throws StorageException {
            when(mockAdapter.getContexts()).thenReturn(Collections.emptySet());

            Set<Resource> result = metadataOps.getContexts();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getStatistics() tests")
    class GetStatisticsTests {

        @Test
        @DisplayName("Should return statistics with correct counts")
        void shouldReturnStatisticsWithCorrectCounts() {
            // Setup mocks
            when(mockAdapter.size()).thenReturn(100);

            Set<Resource> subjects = new HashSet<>(Arrays.asList(
                    mock(Resource.class), mock(Resource.class), mock(Resource.class)
            ));
            when(mockAdapter.getSubjects()).thenReturn(subjects);

            Set<IRI> predicates = new HashSet<>(Arrays.asList(
                    mock(IRI.class), mock(IRI.class)
            ));
            when(mockAdapter.getPredicates()).thenReturn(predicates);

            Set<Value> objects = new HashSet<>(Arrays.asList(
                    mock(Value.class), mock(Value.class), mock(Value.class), mock(Value.class)
            ));
            when(mockAdapter.getObjects()).thenReturn(objects);

            Set<Resource> contexts = new HashSet<>(Collections.singletonList(
                    mock(Resource.class)
            ));
            when(mockAdapter.getContexts()).thenReturn(contexts);

            // Get statistics
            StorageStatistics stats = metadataOps.getStatistics();

            // Verify
            assertEquals(100, stats.statementCount());
            assertEquals(3, stats.subjectCount());
            assertEquals(2, stats.predicateCount());
            assertEquals(4, stats.objectCount());
            assertEquals(1, stats.contextCount());

            verify(mockAdapter).size();
            verify(mockAdapter).getSubjects();
            verify(mockAdapter).getPredicates();
            verify(mockAdapter).getObjects();
            verify(mockAdapter).getContexts();
        }

        @Test
        @DisplayName("Should return zero statistics for empty graph")
        void shouldReturnZeroStatisticsForEmptyGraph() {
            when(mockAdapter.size()).thenReturn(0);
            when(mockAdapter.getSubjects()).thenReturn(Collections.emptySet());
            when(mockAdapter.getPredicates()).thenReturn(Collections.emptySet());
            when(mockAdapter.getObjects()).thenReturn(Collections.emptySet());
            when(mockAdapter.getContexts()).thenReturn(Collections.emptySet());

            StorageStatistics stats = metadataOps.getStatistics();

            assertEquals(0, stats.statementCount());
            assertEquals(0, stats.subjectCount());
            assertEquals(0, stats.predicateCount());
            assertEquals(0, stats.objectCount());
            assertEquals(0, stats.contextCount());
        }

        @Test
        @DisplayName("Should call adapter methods for statistics")
        void shouldCallAdapterMethodsForStatistics() {
            // Setup with empty sets to simplify
            when(mockAdapter.size()).thenReturn(0);
            when(mockAdapter.getSubjects()).thenReturn(Collections.emptySet());
            when(mockAdapter.getPredicates()).thenReturn(Collections.emptySet());
            when(mockAdapter.getObjects()).thenReturn(Collections.emptySet());
            when(mockAdapter.getContexts()).thenReturn(Collections.emptySet());

            metadataOps.getStatistics();

            // Verify all methods were called
            verify(mockAdapter, times(1)).size();
            verify(mockAdapter, times(1)).getSubjects();
            verify(mockAdapter, times(1)).getPredicates();
            verify(mockAdapter, times(1)).getObjects();
            verify(mockAdapter, times(1)).getContexts();
        }
    }
}
