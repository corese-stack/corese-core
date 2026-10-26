package fr.inria.corese.core.next.storage.api.transaction;


import fr.inria.corese.core.next.storage.api.exception.StorageException;

import java.util.Set;

/**
 * Transaction boundary and isolation management for an RDF storage session.
 *
 * <h2>Session Scoping &amp; Operation Participation</h2>
 * <p>A {@link TransactionManager} instance is obtained from a {@link fr.inria.corese.core.next.storage.api.StorageManager}
 * session via {@link fr.inria.corese.core.next.storage.api.StorageManager#transactions()}.
 * Any transaction started through this manager implicitly governs all subsequent read and write
 * operations performed on that same session (via {@link fr.inria.corese.core.next.storage.api.StorageManager#queries()}
 * and {@link fr.inria.corese.core.next.storage.api.StorageManager#mutations()}).</p>
 *
 * <h2>Snapshot Isolation Semantics</h2>
 * <p>Backends supporting snapshot isolation capture a consistent point-in-time view
 * of the database upon transaction start. Uncommitted writes within the transaction are visible
 * to the owning session but remain completely hidden from concurrent sessions until committed.</p>
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
     * Returns the isolation levels supported by this manager.
     *
     * @return Set of supported isolation levels
     */
    Set<IsolationLevel> getSupportedIsolationLevels();
}
