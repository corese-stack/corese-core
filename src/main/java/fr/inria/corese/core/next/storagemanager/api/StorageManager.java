package fr.inria.corese.core.next.storagemanager.api;

import fr.inria.corese.core.next.storagemanager.api.lifecycle.StorageLifecycle;
import fr.inria.corese.core.next.storagemanager.api.transaction.TransactionManager;

/**
 * Main interface for RDF storage management.
 *
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