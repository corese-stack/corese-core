package fr.inria.corese.core.storage.api.dataManager.transaction;

import fr.inria.corese.core.storage.api.dataManager.support.exception.DataManagerException;

/**
 * Handle representing an active transaction.
 */
public interface Transaction extends AutoCloseable {

    /**
     * Returns the unique identifier of this transaction.
     *
     * @return Transaction unique ID
     */
    String getId();

    /**
     * Returns the isolation level of this transaction.
     *
     * @return Isolation level
     */
    IsolationLevel getIsolationLevel();


    /**
     * Commits (validates) the transaction.
     * All modifications made in this transaction become permanent.
     *
     * @throws DataManagerException  if commit fails
     * @throws IllegalStateException if transaction is no longer active
     */
    void commit() throws DataManagerException;

    /**
     * Rolls back (cancels) the transaction.
     * All modifications made in this transaction are cancelled.
     *
     * @throws DataManagerException  if rollback fails
     * @throws IllegalStateException if transaction is no longer active
     */
    void rollback() throws DataManagerException;

    /**
     * Checks if the transaction is still active.
     * A transaction is active if it has been created but not yet committed or rolled back.
     *
     * @return true if transaction is active, false otherwise
     */
    boolean isActive();

    /**
     * Returns the current transaction state.
     *
     * @return Transaction state
     */
    TransactionState getState();


    /**
     * Closes the transaction.
     * If transaction is still active, performs automatic rollback.
     *
     * @throws DataManagerException if close fails
     */
    @Override
    void close() throws DataManagerException;


}