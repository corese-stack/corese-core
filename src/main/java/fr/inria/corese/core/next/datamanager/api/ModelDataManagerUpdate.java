package fr.inria.corese.core.next.datamanager.api;

import fr.inria.corese.core.next.datamanager.api.operations.BulkOperations;
import fr.inria.corese.core.next.datamanager.api.operations.MutationOperations;

/**
 * Update operations for Model DataManager.
 * Groups all write operations.
 */
public interface ModelDataManagerUpdate {

    /**
     * Returns the mutation operations handler.
     */
    MutationOperations getMutationOperations();

    /**
     * Returns the bulk operations handler.
     */
    BulkOperations getBulkOperations();
}
