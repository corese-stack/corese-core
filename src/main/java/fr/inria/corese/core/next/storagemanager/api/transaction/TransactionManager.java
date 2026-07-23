package fr.inria.corese.core.next.storagemanager.api.transaction;


import fr.inria.corese.core.next.storagemanager.api.support.exception.StorageException;

import java.util.Optional;
import java.util.Set;

/**
 * Transaction management for the StorageManager.
 */
public interface TransactionManager {

    /**
     * Checks if this StorageManager supports transactions.
     *
     * @return true if transactions are supported, false otherwise
     */
    boolean supportsTransactions();

    /**
     * Starts a new transaction with the default isolation level.
     *
     * @return Created transaction handle
     * @throws StorageException          if transaction cannot be started
     * @throws UnsupportedOperationException if transactions are not supported
     */
    Transaction beginTransaction() throws StorageException;

    /**
     * Starts a new transaction with a specific isolation level.
     *
     * @param isolationLevel Desired isolation level
     * @return Created transaction handle
     * @throws StorageException          if transaction cannot be started
     * @throws UnsupportedOperationException if transactions are not supported
     * @throws IllegalArgumentException      if isolation level is not supported
     */
    Transaction beginTransaction(IsolationLevel isolationLevel) throws StorageException;

    /**
     * Gets the current transaction of the current thread.
     *
     * @return Current transaction, or Optional.empty() if no active transaction
     */
    Optional<Transaction> getCurrentTransaction();

    /**
     * Returns the default isolation level used for new transactions.
     *
     * @return Default isolation level
     */
    IsolationLevel getDefaultIsolationLevel();

    /**
     * Returns the isolation levels supported by this manager.
     *
     * @return Set of supported isolation levels
     */
    Set<IsolationLevel> getSupportedIsolationLevels();
}