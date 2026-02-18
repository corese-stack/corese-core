package fr.inria.corese.core.next.datamanager.api;

import fr.inria.corese.core.next.datamanager.api.operations.MetadataOperations;
import fr.inria.corese.core.next.datamanager.api.operations.QueryOperations;

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
