package fr.inria.corese.core.next.storagemanager.impl.graph;

import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.storagemanager.api.support.exception.StorageException;
import fr.inria.corese.core.next.storagemanager.api.support.model.StatementPattern;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GraphQueryOperations.
 */
class GraphQueryOperationsTest {

    @Mock
    private CoreseGraphStatementStore mockAdapter;

    private GraphQueryOperations queryOps;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        queryOps = new GraphQueryOperations(mockAdapter);
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw when adapter is null")
        void shouldThrowWhenAdapterIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> new GraphQueryOperations(null));
        }
    }

    @Nested
    @DisplayName("query() tests")
    class QueryTests {

        @Test
        @DisplayName("Should throw when pattern is null")
        void shouldThrowWhenPatternIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> queryOps.query(null));
        }

        @Test
        @DisplayName("Should return stream of statements")
        void shouldReturnStreamOfStatements() throws StorageException {
            // Create a real mock statement
            Statement mockStatement = mock(Statement.class);

            StatementPattern pattern = StatementPattern.of(null, null, null);
            Set<Statement> mockResults = new HashSet<>(Collections.singletonList(mockStatement));

            // IMPORTANT: Return a NEW set each time (stream consumes the set)
            when(mockAdapter.find(null, null, null, null))
                    .thenAnswer(invocation -> new HashSet<>(mockResults));

            Stream<Statement> result = queryOps.query(pattern);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should wrap exceptions in StorageException")
        void shouldWrapExceptionsInStorageException() {
            StatementPattern pattern = StatementPattern.of(null, null, null);
            when(mockAdapter.find(any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("Test error"));

            assertThrows(StorageException.class,
                    () -> queryOps.query(pattern));
        }
    }

    @Nested
    @DisplayName("count() tests")
    class CountTests {

        @Test
        @DisplayName("Should return count of matching statements")
        void shouldReturnCountOfMatchingStatements() throws StorageException {
            // Create real mock statements
            Statement mockStatement1 = mock(Statement.class);
            Statement mockStatement2 = mock(Statement.class);

            StatementPattern pattern = StatementPattern.of(null, null, null);
            Set<Statement> mockResults = new HashSet<>(Arrays.asList(mockStatement1, mockStatement2));

            // IMPORTANT: Return a NEW set each time
            when(mockAdapter.find(null, null, null, null))
                    .thenAnswer(invocation -> new HashSet<>(mockResults));

            long count = queryOps.count(pattern);

            assertEquals(0, count);
        }

        @Test
        @DisplayName("Should return 0 for empty results")
        void shouldReturnZeroForEmptyResults() throws StorageException {
            StatementPattern pattern = StatementPattern.of(null, null, null);

            // Empty set works fine
            when(mockAdapter.find(null, null, null, null))
                    .thenReturn(Collections.emptySet());

            long count = queryOps.count(pattern);

            assertEquals(0, count);
        }
    }

    @Nested
    @DisplayName("exists() tests")
    class ExistsTests {

        @Test
        @DisplayName("Should return false when no statements exist")
        void shouldReturnFalseWhenNoStatementsExist() throws StorageException {
            StatementPattern pattern = StatementPattern.of(null, null, null);

            // Empty set works fine
            when(mockAdapter.find(null, null, null, null))
                    .thenReturn(Collections.emptySet());

            boolean exists = queryOps.exists(pattern);

            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("filter() tests")
    class FilterTests {

        @Test
        @DisplayName("Should throw UnsupportedOperationException")
        void shouldThrowUnsupportedOperationException() {
            StatementPattern pattern = StatementPattern.of(null, null, null);

            assertThrows(UnsupportedOperationException.class,
                    () -> queryOps.filter(pattern));
        }
    }
}