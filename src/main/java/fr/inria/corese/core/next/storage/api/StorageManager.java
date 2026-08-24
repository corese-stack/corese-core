package fr.inria.corese.core.next.storage.api;

import fr.inria.corese.core.next.storage.api.lifecycle.StorageLifecycle;
import fr.inria.corese.core.next.storage.api.transaction.TransactionManager;

/**
 * Main interface for RDF storage management.
 * Combines read-only queries, update mutations, transaction management, and lifecycle handling.
 */
public interface StorageManager extends StorageManagerRead, StorageManagerUpdate {

    /**
     * Returns the transaction manager for this storage.
     *
     * @return the {@link TransactionManager}
     */
    TransactionManager getTransactionManager();

    /**
     * Returns the lifecycle manager for this storage.
     *
     * @return the {@link StorageLifecycle}
     */
    StorageLifecycle getLifecycle();
}