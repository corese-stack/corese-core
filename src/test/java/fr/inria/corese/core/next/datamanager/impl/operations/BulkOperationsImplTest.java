package fr.inria.corese.core.next.datamanager.impl.operations;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.datamanager.api.support.exception.DataManagerException;
import fr.inria.corese.core.next.datamanager.api.support.model.MutationResult;
import fr.inria.corese.core.next.datamanager.api.support.model.StatementPattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BulkOperationsImpl.
 */
@DisplayName("BulkOperationsImpl Tests")
class BulkOperationsImplTest {

    @Mock
    private Model model;

    @Mock
    private Statement stmt1, stmt2, stmt3;

    @Mock
    private Resource context;

    private BulkOperationsImpl bulkOps;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bulkOps = new BulkOperationsImpl(model);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when constructed with null model")
    void testConstructorNullModel() {
        assertThrows(IllegalArgumentException.class,
                () -> new BulkOperationsImpl(null));
    }

    @Test
    @DisplayName("Should successfully insert all statements in batch")
    void testInsertBatchSuccess() throws DataManagerException {
        when(model.add(stmt1)).thenReturn(true);
        when(model.add(stmt2)).thenReturn(true);
        when(model.add(stmt3)).thenReturn(true);

        List<Statement> statements = Arrays.asList(stmt1, stmt2, stmt3);
        MutationResult result = bulkOps.insertBatch(statements);

        assertTrue(result.isSuccess());
        assertEquals(3, result.getTotalAttempted());
        assertEquals(3, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());

        verify(model).add(stmt1);
        verify(model).add(stmt2);
        verify(model).add(stmt3);
    }

    @Test
    @DisplayName("Should handle partial success when some statements already exist")
    void testInsertBatchPartialSuccess() throws DataManagerException {
        when(model.add(stmt1)).thenReturn(true);
        when(model.add(stmt2)).thenReturn(false); // Already exists
        when(model.add(stmt3)).thenReturn(true);

        List<Statement> statements = Arrays.asList(stmt1, stmt2, stmt3);
        MutationResult result = bulkOps.insertBatch(statements);

        assertFalse(result.isSuccess());
        assertEquals(3, result.getTotalAttempted());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when insertBatch called with null list")
    void testInsertBatchNullList() {
        assertThrows(IllegalArgumentException.class,
                () -> bulkOps.insertBatch(null));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when insertBatch called with empty list")
    void testInsertBatchEmptyList() {
        assertThrows(IllegalArgumentException.class,
                () -> bulkOps.insertBatch(Collections.emptyList()));
    }

    @Test
    @DisplayName("Should successfully delete all statements in batch")
    void testDeleteBatchSuccess() throws DataManagerException {
        when(model.remove(stmt1)).thenReturn(true);
        when(model.remove(stmt2)).thenReturn(true);

        List<Statement> statements = Arrays.asList(stmt1, stmt2);
        MutationResult result = bulkOps.deleteBatch(statements);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getTotalAttempted());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());

        verify(model).remove(stmt1);
        verify(model).remove(stmt2);
    }

    @Test
    @DisplayName("Should report failure when statement to delete is not found")
    void testDeleteBatchNotFound() throws DataManagerException {
        when(model.remove(stmt1)).thenReturn(false); // Not found

        List<Statement> statements = List.of(stmt1);
        MutationResult result = bulkOps.deleteBatch(statements);

        assertFalse(result.isSuccess());
        assertEquals(1, result.getTotalAttempted());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when deleteBatch called with null list")
    void testDeleteBatchNullList() {
        assertThrows(IllegalArgumentException.class,
                () -> bulkOps.deleteBatch(null));
    }

    @Test
    @DisplayName("Should delete statements matching the given pattern")
    void testDeleteByPattern() throws DataManagerException {
        Resource subject = mock(Resource.class);
        IRI predicate = mock(IRI.class);
        Value object = mock(Value.class);
        Resource[] contexts = new Resource[]{context};

        StatementPattern pattern = StatementPattern.builder()
                .subject(subject)
                .predicate(predicate)
                .object(object)
                .contexts(contexts)
                .build();

        when(model.remove(subject, predicate, object, contexts)).thenReturn(true);

        MutationResult result = bulkOps.deleteByPattern(pattern);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getSuccessCount());
        verify(model).remove(subject, predicate, object, contexts);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when deleteByPattern called with null pattern")
    void testDeleteByPatternNull() {
        assertThrows(IllegalArgumentException.class,
                () -> bulkOps.deleteByPattern(null));
    }

    @Test
    @DisplayName("Should succeed in silent mode even when clear returns false")
    void testClearContextsSilentMode() throws DataManagerException {
        when(model.clear(any(Resource[].class))).thenReturn(false);

        MutationResult result = bulkOps.clearContexts(List.of(context), true);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle exceptions during batch insert and continue processing")
    void testInsertBatchWithException() throws DataManagerException {
        when(model.add(stmt1)).thenReturn(true);
        when(model.add(stmt2)).thenThrow(new RuntimeException("Test error"));
        when(model.add(stmt3)).thenReturn(true);

        List<Statement> statements = Arrays.asList(stmt1, stmt2, stmt3);
        MutationResult result = bulkOps.insertBatch(statements);

        assertFalse(result.isSuccess());
        assertEquals(3, result.getTotalAttempted());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    @DisplayName("Should track errors when exception occurs during batch delete")
    void testDeleteBatchWithException() throws DataManagerException {
        when(model.remove(stmt1)).thenThrow(new RuntimeException("Test error"));

        List<Statement> statements = List.of(stmt1);
        MutationResult result = bulkOps.deleteBatch(statements);

        assertFalse(result.isSuccess());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getErrors().size());
    }
}