package fr.inria.corese.core.next.storagemanager.impl.memory;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.storagemanager.api.support.exception.StorageException;
import fr.inria.corese.core.next.storagemanager.api.support.model.MutationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MemoryMutationOperations.
 */
class MemoryMutationOperationsTest {

    @Mock
    private MemoryAdapter mockAdapter;

    @Mock
    private Statement mockStatement;

    private MemoryMutationOperations mutationOps;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mutationOps = new MemoryMutationOperations(mockAdapter);
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw when adapter is null")
        void shouldThrowWhenAdapterIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> new MemoryMutationOperations(null));
        }
    }

    @Nested
    @DisplayName("insertStatement() tests")
    class InsertStatementTests {

        @Test
        @DisplayName("Should throw when statement is null")
        void shouldThrowWhenStatementIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> mutationOps.insertStatement(null));
        }

        @Test
        @DisplayName("Should return success when statement is added")
        void shouldReturnSuccessWhenStatementIsAdded() throws StorageException {
            when(mockAdapter.add(mockStatement)).thenReturn(true);

            MutationResult result = mutationOps.insertStatement(mockStatement);

            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains("Inserted"));
            verify(mockAdapter).add(mockStatement);
        }

        @Test
        @DisplayName("Should return success when statement already exists")
        void shouldReturnSuccessWhenStatementAlreadyExists() throws StorageException {
            when(mockAdapter.add(mockStatement)).thenReturn(false);

            MutationResult result = mutationOps.insertStatement(mockStatement);

            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains("Already exists"));
        }

        @Test
        @DisplayName("Should wrap exceptions in StorageException")
        void shouldWrapExceptionsInStorageException() {
            when(mockAdapter.add(mockStatement))
                    .thenThrow(new RuntimeException("Test error"));

            assertThrows(StorageException.class,
                    () -> mutationOps.insertStatement(mockStatement));
        }
    }

    @Nested
    @DisplayName("insertStatement(components) tests")
    class InsertStatementComponentsTests {

        @Mock
        private Resource mockSubject;

        @Mock
        private IRI mockPredicate;

        @Mock
        private Value mockObject;

        @Test
        @DisplayName("Should throw UnsupportedOperationException")
        void shouldThrowUnsupportedOperationException() {
            assertThrows(UnsupportedOperationException.class, () -> mutationOps.insertStatement(mockSubject, mockPredicate, mockObject));
        }
    }

    @Nested
    @DisplayName("deleteStatement() tests")
    class DeleteStatementTests {

        @Test
        @DisplayName("Should throw when statement is null")
        void shouldThrowWhenStatementIsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> mutationOps.deleteStatement(null));
        }

        @Test
        @DisplayName("Should return success when statement is deleted")
        void shouldReturnSuccessWhenStatementIsDeleted() throws StorageException {
            when(mockAdapter.remove(mockStatement)).thenReturn(true);

            MutationResult result = mutationOps.deleteStatement(mockStatement);

            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains("Deleted"));
            verify(mockAdapter).remove(mockStatement);
        }

        @Test
        @DisplayName("Should return failure when statement not found")
        void shouldReturnFailureWhenStatementNotFound() throws StorageException {
            when(mockAdapter.remove(mockStatement)).thenReturn(false);

            MutationResult result = mutationOps.deleteStatement(mockStatement);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("Not found"));
        }
    }

    @Nested
    @DisplayName("deleteStatements(pattern) tests")
    class DeleteStatementsByPatternTests {


        @Test
        @DisplayName("Should return zero when no matches")
        void shouldReturnZeroWhenNoMatches() throws StorageException {
            when(mockAdapter.find(null, null, null, null))
                    .thenReturn(Collections.emptySet());

            MutationResult result = mutationOps.deleteStatements(null, null, null);

            assertEquals(0, result.getSuccessCount());
            assertEquals(0, result.getTotalAttempted());
        }
    }

    @Nested
    @DisplayName("clear() tests")
    class ClearTests {

        @Test
        @DisplayName("Should clear entire graph when no contexts provided")
        void shouldClearEntireGraphWhenNoContextsProvided() throws StorageException {
            when(mockAdapter.size()).thenReturn(100, 0);

            MutationResult result = mutationOps.clear();

            assertEquals(100, result.getSuccessCount());
            verify(mockAdapter).clear();
        }

        @Test
        @DisplayName("Should clear specific contexts when provided")
        void shouldClearSpecificContextsWhenProvided() throws StorageException {
            Resource ctx1 = mock(Resource.class);
            Resource ctx2 = mock(Resource.class);
            when(mockAdapter.size()).thenReturn(100, 50);

            MutationResult result = mutationOps.clear(ctx1, ctx2);

            assertEquals(50, result.getSuccessCount());
            verify(mockAdapter).clearContext(ctx1);
            verify(mockAdapter).clearContext(ctx2);
            verify(mockAdapter, never()).clear();
        }
    }
}
