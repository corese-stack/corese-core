package fr.inria.corese.core.next.storagemanager.impl.memory;

import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.storagemanager.api.support.exception.StorageException;
import fr.inria.corese.core.next.storagemanager.api.support.model.StatementPattern;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MemoryQueryOperations.
 */
class MemoryQueryOperationsTest {

    @Mock
    private InMemoryStatementStore mockAdapter;
    
    private MemoryQueryOperations queryOps;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        queryOps = new MemoryQueryOperations(mockAdapter);
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Should throw when adapter is null")
        void shouldThrowWhenAdapterIsNull() {
            assertThrows(IllegalArgumentException.class, 
                () -> new MemoryQueryOperations(null));
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
            Statement mockStatement = mock(Statement.class);
            StatementPattern pattern = StatementPattern.of(null, null, null);
            Set<Statement> mockResults = new HashSet<>(Collections.singletonList(mockStatement));
            
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
        @DisplayName("Should return 0 for empty results")
        void shouldReturnZeroForEmptyResults() throws StorageException {
            StatementPattern pattern = StatementPattern.of(null, null, null);
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
