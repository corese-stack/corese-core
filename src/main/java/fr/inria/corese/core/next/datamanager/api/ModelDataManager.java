package fr.inria.corese.core.next.datamanager.api;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.datamanager.api.lifecycle.DataManagerLifecycle;
import fr.inria.corese.core.next.datamanager.api.operations.BulkOperations;
import fr.inria.corese.core.next.datamanager.api.operations.MetadataOperations;
import fr.inria.corese.core.next.datamanager.api.operations.MutationOperations;
import fr.inria.corese.core.next.datamanager.api.operations.QueryOperations;
import fr.inria.corese.core.next.datamanager.api.transaction.TransactionManager;

/**
 * Main interface for the Model DataManager.
 * Provides access to all DataManager operations through specialized operation interfaces.
 */
public interface ModelDataManager {

    /**
     * Returns the query operations handler.
     * Provides read-only operations for querying statements.
     *
     * @return QueryOperations instance
     */
    QueryOperations getQueryOperations();

    /**
     * Returns the mutation operations handler.
     * Provides operations for inserting, updating, and deleting statements.
     *
     * @return MutationOperations instance
     */
    MutationOperations getMutationOperations();

    /**
     * Returns the metadata operations handler.
     * Provides access to model structure information (subjects, predicates, objects, contexts, statistics).
     *
     * @return MetadataOperations instance
     */
    MetadataOperations getMetadataOperations();

    /**
     * Returns the bulk operations handler.
     * Provides batch operations for efficient processing of multiple statements.
     *
     * @return BulkOperations instance
     */
    BulkOperations getBulkOperations();

    /**
     * Returns the transaction manager.
     * Provides transaction support if enabled in configuration.
     *
     * @return TransactionManager instance
     */
    TransactionManager getTransactionManager();

    /**
     * Returns the lifecycle manager.
     * Provides initialization, shutdown, and restart operations.
     *
     * @return DataManagerLifecycle instance
     */
    DataManagerLifecycle getLifecycle();

    /**
     * Returns the underlying Model instance.
     * Provides direct access to the RDF model for advanced use cases.
     *
     * @return Model instance
     */
    Model getModel();
}