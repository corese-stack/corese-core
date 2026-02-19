package fr.inria.corese.core.next.storagemanager.api;

import fr.inria.corese.core.next.storagemanager.api.operations.MetadataOperations;
import fr.inria.corese.core.next.storagemanager.api.operations.QueryOperations;

/**
 * Read operations for Model Storage Manager.
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
