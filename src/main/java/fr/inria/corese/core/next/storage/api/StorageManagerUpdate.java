package fr.inria.corese.core.next.storage.api;

import fr.inria.corese.core.next.storage.api.operations.BulkOperations;
import fr.inria.corese.core.next.storage.api.operations.MutationOperations;

/**
 * Mutation capabilities exposed by a storage manager.
 * Groups all write operations.
 */
public interface StorageManagerUpdate {

    /**
     * Returns the mutation operations handler.
     */
    MutationOperations getMutationOperations();

    /**
     * Returns the bulk operations handler.
     */
    BulkOperations getBulkOperations();
}
