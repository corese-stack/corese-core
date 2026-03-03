package fr.inria.corese.core.next.storagemanager.impl.memory;

import fr.inria.corese.core.next.data.api.*;
import fr.inria.corese.core.next.storagemanager.api.support.exception.StorageException;
import fr.inria.corese.core.next.storagemanager.api.support.model.MutationResult;
import fr.inria.corese.core.next.storagemanager.api.support.model.StatementPattern;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MemoryBulkOperations.
 */
class MemoryBulkOperationsTest {

    @Mock
    private MemoryAdapter mockAdapter;
    
    private MemoryBulkOperations bulkOps;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bulkOps = new MemoryBulkOperations(mockAdapter);
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Should throw when adapter is null")
        void shouldThrowWhenAdapterIsNull() {
            assertThrows(IllegalArgumentException.class, 
                () -> new MemoryBulkOperations(null));
        }
    }

    @Nested
    @DisplayName("insertBatch() tests")
    class InsertBatchTests {
        
        @Test
        @DisplayName("Should throw when statements is null")
        void shouldThrowWhenStatementsIsNull() {
            assertThrows(IllegalArgumentException.class, 
                () -> bulkOps.insertBatch(null));
        }
        
        @Test
        @DisplayName("Should throw when statements is empty")
        void shouldThrowWhenStatementsIsEmpty() {
            assertThrows(IllegalArgumentException.class, 
                () -> bulkOps.insertBatch(Collections.emptyList()));
        }
        
        @Test
        @DisplayName("Should insert all statements successfully")
        void shouldInsertAllStatementsSuccessfully() throws StorageException {
            Statement stmt1 = mock(Statement.class);
            Statement stmt2 = mock(Statement.class);
            Statement stmt3 = mock(Statement.class);
            List<Statement> statements = Arrays.asList(stmt1, stmt2, stmt3);
            
            when(mockAdapter.add(any(Statement.class))).thenReturn(true);
            
            MutationResult result = bulkOps.insertBatch(statements);
            
            assertEquals(3, result.getTotalAttempted());
            assertEquals(3, result.getSuccessCount());
            assertEquals(0, result.getFailureCount());
            verify(mockAdapter, times(3)).add(any(Statement.class));
        }
        

        @Test
        @DisplayName("Should handle exceptions during insert")
        void shouldHandleExceptionsDuringInsert() throws StorageException {
            Statement stmt1 = mock(Statement.class);
            Statement stmt2 = mock(Statement.class);
            Statement stmt3 = mock(Statement.class);
            List<Statement> statements = Arrays.asList(stmt1, stmt2, stmt3);
            
            when(mockAdapter.add(stmt1)).thenReturn(true);
            when(mockAdapter.add(stmt2)).thenThrow(new RuntimeException("Insert error"));
            when(mockAdapter.add(stmt3)).thenReturn(true);
            
            MutationResult result = bulkOps.insertBatch(statements);
            
            assertEquals(3, result.getTotalAttempted());
            assertEquals(2, result.getSuccessCount());
            assertEquals(1, result.getFailureCount());
        }
        
        @Test
        @DisplayName("Should handle all failures")
        void shouldHandleAllFailures() throws StorageException {
            Statement stmt1 = mock(Statement.class);
            Statement stmt2 = mock(Statement.class);
            List<Statement> statements = Arrays.asList(stmt1, stmt2);
            
            when(mockAdapter.add(any(Statement.class))).thenReturn(false);
            
            MutationResult result = bulkOps.insertBatch(statements);
            
            assertEquals(2, result.getTotalAttempted());
            assertEquals(0, result.getSuccessCount());
            assertEquals(2, result.getFailureCount());
        }
    }

    @Nested
    @DisplayName("deleteBatch() tests")
    class DeleteBatchTests {
        
        @Test
        @DisplayName("Should throw when statements is null")
        void shouldThrowWhenStatementsIsNull() {
            assertThrows(IllegalArgumentException.class, 
                () -> bulkOps.deleteBatch(null));
        }
        
        @Test
        @DisplayName("Should throw when statements is empty")
        void shouldThrowWhenStatementsIsEmpty() {
            assertThrows(IllegalArgumentException.class, 
                () -> bulkOps.deleteBatch(Collections.emptyList()));
        }
        
        @Test
        @DisplayName("Should delete all statements successfully")
        void shouldDeleteAllStatementsSuccessfully() throws StorageException {
            Statement stmt1 = mock(Statement.class);
            Statement stmt2 = mock(Statement.class);
            List<Statement> statements = Arrays.asList(stmt1, stmt2);
            
            when(mockAdapter.remove(any(Statement.class))).thenReturn(true);
            
            MutationResult result = bulkOps.deleteBatch(statements);
            
            assertEquals(2, result.getTotalAttempted());
            assertEquals(2, result.getSuccessCount());
            assertEquals(0, result.getFailureCount());
            verify(mockAdapter, times(2)).remove(any(Statement.class));
        }
        

        @Test
        @DisplayName("Should handle exceptions during delete")
        void shouldHandleExceptionsDuringDelete() throws StorageException {
            Statement stmt1 = mock(Statement.class);
            Statement stmt2 = mock(Statement.class);
            List<Statement> statements = Arrays.asList(stmt1, stmt2);
            
            when(mockAdapter.remove(stmt1)).thenReturn(true);
            when(mockAdapter.remove(stmt2)).thenThrow(new RuntimeException("Delete error"));
            
            MutationResult result = bulkOps.deleteBatch(statements);
            
            assertEquals(2, result.getTotalAttempted());
            assertEquals(1, result.getSuccessCount());
            assertEquals(1, result.getFailureCount());
        }
    }

    @Nested
    @DisplayName("deleteByPattern() tests")
    class DeleteByPatternTests {
        
        @Test
        @DisplayName("Should throw when pattern is null")
        void shouldThrowWhenPatternIsNull() {
            assertThrows(IllegalArgumentException.class, 
                () -> bulkOps.deleteByPattern(null));
        }
        

        @Test
        @DisplayName("Should handle no matching statements")
        void shouldHandleNoMatchingStatements() throws StorageException {
            StatementPattern pattern = StatementPattern.of(mock(Resource.class), null, null);
            
            when(mockAdapter.find(any(), any(), any(), any())).thenReturn(Collections.emptySet());
            
            MutationResult result = bulkOps.deleteByPattern(pattern);
            
            assertEquals(0, result.getTotalAttempted());
            assertEquals(0, result.getSuccessCount());
            verify(mockAdapter, never()).remove(any(Statement.class));
        }
        
    }

    @Nested
    @DisplayName("clearContexts() tests")
    class ClearContextsTests {
        
        @Test
        @DisplayName("Should clear entire graph when contexts is null")
        void shouldClearEntireGraphWhenContextsIsNull() throws StorageException {
            when(mockAdapter.size()).thenReturn(100, 0);
            
            MutationResult result = bulkOps.clearContexts(null, false);
            
            assertEquals(100, result.getTotalAttempted());
            assertEquals(100, result.getSuccessCount());
            verify(mockAdapter).clear();
            verify(mockAdapter, never()).clearContext(any());
        }
        
        @Test
        @DisplayName("Should clear entire graph when contexts is empty")
        void shouldClearEntireGraphWhenContextsIsEmpty() throws StorageException {
            when(mockAdapter.size()).thenReturn(50, 0);
            
            MutationResult result = bulkOps.clearContexts(Collections.emptyList(), false);
            
            assertEquals(50, result.getTotalAttempted());
            assertEquals(50, result.getSuccessCount());
            verify(mockAdapter).clear();
            verify(mockAdapter, never()).clearContext(any());
        }
        
        @Test
        @DisplayName("Should clear specific contexts")
        void shouldClearSpecificContexts() throws StorageException {
            Resource ctx1 = mock(Resource.class);
            Resource ctx2 = mock(Resource.class);
            Resource ctx3 = mock(Resource.class);
            List<Resource> contexts = Arrays.asList(ctx1, ctx2, ctx3);
            
            when(mockAdapter.size()).thenReturn(100, 70);
            
            MutationResult result = bulkOps.clearContexts(contexts, false);
            
            assertEquals(30, result.getTotalAttempted());
            assertEquals(30, result.getSuccessCount());
            verify(mockAdapter, never()).clear();
            verify(mockAdapter).clearContext(ctx1);
            verify(mockAdapter).clearContext(ctx2);
            verify(mockAdapter).clearContext(ctx3);
        }
        
        @Test
        @DisplayName("Should handle clearing when no statements deleted")
        void shouldHandleClearingWhenNoStatementsDeleted() throws StorageException {
            Resource ctx = mock(Resource.class);
            List<Resource> contexts = Collections.singletonList(ctx);
            
            when(mockAdapter.size()).thenReturn(10, 10); // No change
            
            MutationResult result = bulkOps.clearContexts(contexts, false);
            
            assertEquals(0, result.getTotalAttempted());
            assertEquals(0, result.getSuccessCount());
        }
        
        @Test
        @DisplayName("Should ignore silent parameter")
        void shouldIgnoreSilentParameter() throws StorageException {
            when(mockAdapter.size()).thenReturn(5, 0);
            
            // Silent = true should behave the same as false
            MutationResult result = bulkOps.clearContexts(null, true);
            
            assertEquals(5, result.getSuccessCount());
            verify(mockAdapter).clear();
        }
    }
}
