package fr.inria.corese.core.next.storage.api;

import fr.inria.corese.core.next.storage.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.storage.api.lifecycle.StorageLifecycle;
import fr.inria.corese.core.next.storage.api.operations.MetadataOperations;
import fr.inria.corese.core.next.storage.api.operations.MutationOperations;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;
import fr.inria.corese.core.next.storage.api.transaction.TransactionManager;

/**
 * Service-provider contract for an RDF storage backend.
 *
 * <p>Repository users normally interact with the query API rather than this
 * interface. Storage implementations expose focused capabilities for reads,
 * writes, metadata, transactions, and lifecycle management.</p>
 */
public interface StorageManager extends AutoCloseable {

    /** @return statement query operations */
    QueryOperations queries();

    /** @return statement mutation operations */
    MutationOperations mutations();

    /** @return storage metadata operations */
    MetadataOperations metadata();

    /**
     * Returns the transaction manager for this storage.
     *
     * @return the {@link TransactionManager}
     */
    TransactionManager transactions();

    /**
     * Returns the lifecycle manager for this storage.
     *
     * @return the {@link StorageLifecycle}
     */
    StorageLifecycle lifecycle();

    /** @return whether this backend is initialized and ready */
    default boolean isOpen() {
        return lifecycle().getState() == LifecycleState.RUNNING;
    }

    /**
     * Shuts down this backend when it is running. Closing an uninitialized or
     * already-closed backend is a no-op.
     */
    @Override
    default void close() {
        if (isOpen()) {
            lifecycle().shutdown();
        }
    }
}
