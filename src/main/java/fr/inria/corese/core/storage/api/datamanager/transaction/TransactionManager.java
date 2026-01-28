package fr.inria.corese.core.storage.api.datamanager.transaction;

import fr.inria.corese.core.storage.api.datamanager.support.exception.DataManagerException;

import java.util.Optional;
import java.util.Set;

/**
 * Transaction management for the DataManager.
 */
public interface TransactionManager {

    /**
     * Checks if this DataManager supports transactions.
     *
     * @return true if transactions are supported, false otherwise
     */
    boolean supportsTransactions();

    /**
     * Starts a new transaction with the default isolation level.
     *
     * @return Created transaction handle
     * @throws DataManagerException          if transaction cannot be started
     * @throws UnsupportedOperationException if transactions are not supported
     */
    Transaction beginTransaction() throws DataManagerException;

    /**
     * Starts a new transaction with a specific isolation level.
     *
     * @param isolationLevel Desired isolation level
     * @return Created transaction handle
     * @throws DataManagerException          if transaction cannot be started
     * @throws UnsupportedOperationException if transactions are not supported
     * @throws IllegalArgumentException      if isolation level is not supported
     */
    Transaction beginTransaction(IsolationLevel isolationLevel) throws DataManagerException;

    /**
     * Gets the current transaction of the current thread.
     *
     * @return Current transaction, or Optional.empty() if no active transaction
     */
    Optional<Transaction> getCurrentTransaction();

    /**
     * Checks if a transaction is active on the current thread.
     *
     * @return true if a transaction is active
     */
    default boolean hasActiveTransaction() {
        return getCurrentTransaction().map(Transaction::isActive).orElse(false);
    }

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