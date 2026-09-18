package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.storage.api.transaction.IsolationLevel;
import fr.inria.corese.core.next.storage.api.transaction.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryTransactionManagerTest {
    private InMemoryStatementStore store;
    private MemoryTransactionManager transactions;
    private Statement statement;

    @BeforeEach
    void setUp() {
        store = new InMemoryStatementStore();
        transactions = new MemoryTransactionManager(store);
        var factory = Values.factory();
        statement = factory.createStatement(factory.createIRI("urn:s"), factory.createIRI("urn:p"),
                factory.createIRI("urn:o"), factory.createIRI("urn:g"));
    }

    @Test
    void commitPublishesStatementsAndGraphNamesTogether() {
        try (Transaction transaction = transactions.beginTransaction()) {
            store.add(statement);
            assertEquals(1, store.size());
            assertEquals(0, CompletableFuture.supplyAsync(store::size).join());
            transaction.commit();
        }
        assertEquals(1, store.size());
        assertEquals(Set.of(statement.getContext()), store.getContexts());
    }

    @Test
    void closeRollsBackIncludingEmptyGraphNames() {
        try (Transaction transaction = transactions.beginTransaction()) {
            assertTrue(transaction.isActive());
            store.createGraph(statement.getContext());
            store.add(statement);
        }
        assertEquals(0, store.size());
        assertTrue(store.getContexts().isEmpty());
    }

    @Test
    void concurrentCommitIsDetectedWithoutLosingTheOtherWriter() {
        try (Transaction transaction = transactions.beginTransaction()) {
            CompletableFuture.runAsync(() -> store.add(statement)).join();
            assertEquals(0, store.size());
            assertThrows(IllegalStateException.class, transaction::commit);
        }
        assertEquals(1, store.size());
    }

    @Test
    void transactionLifecycleAndIsolationAreExplicit() {
        assertTrue(transactions.supportsTransactions());
        assertEquals(Set.of(IsolationLevel.SERIALIZABLE), transactions.getSupportedIsolationLevels());
        assertThrows(IllegalArgumentException.class, () -> transactions.beginTransaction(IsolationLevel.READ_UNCOMMITTED));
        try (Transaction transaction = transactions.beginTransaction()) {
            assertThrows(IllegalStateException.class, transactions::beginTransaction);
            transaction.rollback();
            assertFalse(transaction.isActive());
            assertThrows(IllegalStateException.class, transaction::commit);
        }
    }
}
