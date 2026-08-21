package fr.inria.corese.core.next.storage.api.model;

import fr.inria.corese.core.next.data.api.model.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MutationResult.
 */
@DisplayName("MutationResult Tests")
class MutationResultTest {

    @Test
    @DisplayName("Should create successful single mutation result")
    void testSuccessSingle() {
        Statement stmt = Mockito.mock(Statement.class);
        MutationResult result = MutationResult.success(stmt);

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertFalse(result.isBulk());
        assertEquals(Optional.of(stmt), result.getAffectedStatement());
        assertEquals(java.util.List.of(stmt), result.getAffectedStatements());
        assertEquals(1, result.getTotalAttempted());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
    }

    @Test
    @DisplayName("Should create successful result with custom message")
    void testSuccessWithMessage() {
        Statement stmt = Mockito.mock(Statement.class);
        MutationResult result = MutationResult.success(stmt, "Custom message");

        assertTrue(result.isSuccess());
        assertEquals("Custom message", result.getMessage());
    }

    @Test
    @DisplayName("Should create failure result with message")
    void testFailure() {
        MutationResult result = MutationResult.failure("Operation failed");

        assertTrue(result.isFailure());
        assertFalse(result.isSuccess());
        assertEquals("Operation failed", result.getMessage());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
    }

    @Test
    @DisplayName("Should create failure result with error exception")
    void testFailureWithError() {
        Exception error = new RuntimeException("Test error");
        MutationResult result = MutationResult.failure("Failed", error);

        assertTrue(result.isFailure());
        assertTrue(result.getError().isPresent());
        assertEquals(error, result.getError().get());
    }

    @Test
    @DisplayName("Should build bulk result with mixed successes and failures")
    void testBulkBuilder() {
        Statement stmt1 = Mockito.mock(Statement.class);
        Statement stmt2 = Mockito.mock(Statement.class);

        MutationResult result = MutationResult.bulkBuilder()
                .totalAttempted(3)
                .addSuccess(stmt1)
                .addSuccess(stmt2)
                .addFailure(null, "Failed to insert")
                .message("Bulk operation")
                .build();

        assertTrue(result.isBulk());
        assertFalse(result.isSuccess()); // Not all succeeded
        assertEquals(3, result.getTotalAttempted());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals("Bulk operation", result.getMessage());
    }

    @Test
    @DisplayName("Should recognize complete success in bulk operations")
    void testBulkCompleteSuccess() {
        Statement stmt1 = Mockito.mock(Statement.class);
        Statement stmt2 = Mockito.mock(Statement.class);

        MutationResult result = MutationResult.bulkBuilder()
                .totalAttempted(2)
                .addSuccess(stmt1)
                .addSuccess(stmt2)
                .build();

        assertTrue(result.isSuccess());
        assertTrue(result.isCompleteSuccess());
        assertEquals(1.0, result.getSuccessRate(), 0.01);
    }

    @Test
    @DisplayName("Should calculate correct success rate")
    void testSuccessRate() {
        MutationResult result = MutationResult.bulkBuilder()
                .totalAttempted(10)
                .successCount(7)
                .failureCount(3)
                .build();

        assertEquals(0.7, result.getSuccessRate(), 0.01);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when getting single statement from bulk result")
    void testGetAffectedStatementThrowsForMultiple() {
        Statement stmt1 = Mockito.mock(Statement.class);
        Statement stmt2 = Mockito.mock(Statement.class);

        MutationResult result = MutationResult.bulkBuilder()
                .totalAttempted(2)
                .addSuccess(stmt1)
                .addSuccess(stmt2)
                .build();

        assertThrows(IllegalStateException.class, result::getAffectedStatement);
    }

    @Test
    @DisplayName("Should track error details for failed mutations")
    void testErrorTracking() {
        Statement stmt = Mockito.mock(Statement.class);
        Exception error = new RuntimeException("Test");

        MutationResult result = MutationResult.bulkBuilder()
                .totalAttempted(1)
                .addFailure(stmt, "Error occurred", error)
                .build();

        assertEquals(1, result.getErrors().size());
        MutationResult.MutationError mutError = result.getErrors().getFirst();
        assertEquals(stmt, mutError.getStatement());
        assertEquals("Error occurred", mutError.getMessage());
        assertEquals(error, mutError.getCause());
    }
}
