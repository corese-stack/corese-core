package fr.inria.corese.core.next.storage.api;

import fr.inria.corese.core.next.storage.api.operations.MetadataOperations;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;

/**
 * Read-only capabilities exposed by a storage manager.
 * Groups all read-only operations.
 */
public interface StorageManagerRead {

    /**
     * Returns the query operations handler.
     */
    QueryOperations getQueryOperations();

    /**
     * Returns the metadata operations handler.
     */
    MetadataOperations getMetadataOperations();
}
