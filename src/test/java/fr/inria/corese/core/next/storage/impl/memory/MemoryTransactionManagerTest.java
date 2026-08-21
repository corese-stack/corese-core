package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.storage.api.transaction.IsolationLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MemoryTransactionManager tests")
class MemoryTransactionManagerTest {

    private MemoryTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        transactionManager = new MemoryTransactionManager();
    }

    @Test
    @DisplayName("Should not support transactions")
    void shouldNotSupportTransactions() {
        assertFalse(transactionManager.supportsTransactions());
    }

    @Test
    @DisplayName("Should throw when beginning transaction")
    void shouldThrowWhenBeginningTransaction() {
        assertThrows(UnsupportedOperationException.class,
                () -> transactionManager.beginTransaction());
    }

    @Test
    @DisplayName("Should throw when beginning transaction with isolation level")
    void shouldThrowWhenBeginningTransactionWithIsolationLevel() {
        assertThrows(UnsupportedOperationException.class,
                () -> transactionManager.beginTransaction(IsolationLevel.READ_COMMITTED));
    }

    @Test
    @DisplayName("Should return empty current transaction")
    void shouldReturnEmptyCurrentTransaction() {
        assertTrue(transactionManager.getCurrentTransaction().isEmpty());
    }

    @Test
    @DisplayName("Should return READ_COMMITTED as default isolation level")
    void shouldReturnReadCommittedAsDefaultIsolationLevel() {
        assertEquals(IsolationLevel.READ_COMMITTED,
                transactionManager.getDefaultIsolationLevel());
    }

    @Test
    @DisplayName("Should return empty set of supported isolation levels")
    void shouldReturnEmptySetOfSupportedIsolationLevels() {
        assertTrue(transactionManager.getSupportedIsolationLevels().isEmpty());
    }
}