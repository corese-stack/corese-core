package fr.inria.corese.core.next.query.impl.repository;

import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.repository.Repository;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.transaction.Transaction;
import fr.inria.corese.core.next.storage.api.transaction.TransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoreseRepositoryConnectionTransactionTest {

    private CoreseRepositoryConnection connection;
    private TransactionManager transactionManager;
    private Transaction transaction;
    private AtomicBoolean active;

    @BeforeEach
    void setUp() {
        Repository repository = mock(Repository.class);
        StorageManager storage = mock(StorageManager.class);
        transactionManager = mock(TransactionManager.class);
        transaction = mock(Transaction.class);
        active = new AtomicBoolean();

        when(repository.isOpen()).thenReturn(true);
        when(storage.getTransactionManager()).thenReturn(transactionManager);
        when(transactionManager.supportsTransactions()).thenReturn(true);
        when(transactionManager.beginTransaction()).thenAnswer(invocation -> {
            active.set(true);
            return transaction;
        });
        when(transaction.isActive()).thenAnswer(invocation -> active.get());
        doAnswer(invocation -> {
            active.set(false);
            return null;
        }).when(transaction).commit();
        doAnswer(invocation -> {
            active.set(false);
            return null;
        }).when(transaction).rollback();
        doAnswer(invocation -> {
            active.set(false);
            return null;
        }).when(transaction).close();

        connection = new CoreseRepositoryConnection(repository, storage);
    }

    @Test
    void beginAndCommitDelegateToStorageTransaction() {
        connection.begin();
        assertTrue(connection.isActive());
        assertThrows(RepositoryException.class, connection::begin);

        connection.commit();

        assertFalse(connection.isActive());
        verify(transaction).commit();
    }

    @Test
    void rollbackRequiresAndEndsAnActiveTransaction() {
        assertThrows(RepositoryException.class, connection::rollback);

        connection.begin();
        connection.rollback();

        assertFalse(connection.isActive());
        verify(transaction).rollback();
    }

    @Test
    void closeRollsBackThroughTransactionClose() {
        connection.begin();

        connection.close();

        assertFalse(connection.isOpen());
        verify(transaction).close();
    }
}
