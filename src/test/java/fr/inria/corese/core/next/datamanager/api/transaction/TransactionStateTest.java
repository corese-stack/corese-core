package fr.inria.corese.core.next.datamanager.api.transaction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TransactionState enum.
 */
@DisplayName("TransactionState Enum Tests")
class TransactionStateTest {

    @Test
    @DisplayName("Should have all 4 transaction states defined")
    void testAllStatesExist() {
        assertNotNull(TransactionState.ACTIVE);
        assertNotNull(TransactionState.COMMITTED);
        assertNotNull(TransactionState.ROLLED_BACK);
        assertNotNull(TransactionState.FAILED);
    }

    @Test
    @DisplayName("Should return true for isActive() only when state is ACTIVE")
    void testIsActive() {
        assertTrue(TransactionState.ACTIVE.isActive());

        assertFalse(TransactionState.COMMITTED.isActive());
        assertFalse(TransactionState.ROLLED_BACK.isActive());
        assertFalse(TransactionState.FAILED.isActive());
    }

    @Test
    @DisplayName("Should return correct description for each state")
    void testDescriptions() {
        assertEquals("Active", TransactionState.ACTIVE.getDescription());
        assertEquals("Committed", TransactionState.COMMITTED.getDescription());
        assertEquals("Rolled back", TransactionState.ROLLED_BACK.getDescription());
        assertEquals("Failed", TransactionState.FAILED.getDescription());
    }

    @Test
    @DisplayName("Should have exactly 4 enum values")
    void testEnumValues() {
        TransactionState[] states = TransactionState.values();
        assertEquals(4, states.length);
    }

    @Test
    @DisplayName("Should correctly resolve valueOf() for valid and invalid state names")
    void testValueOf() {
        assertEquals(TransactionState.ACTIVE, TransactionState.valueOf("ACTIVE"));
        assertEquals(TransactionState.COMMITTED, TransactionState.valueOf("COMMITTED"));

        assertThrows(IllegalArgumentException.class,
                () -> TransactionState.valueOf("INVALID"));
    }
}