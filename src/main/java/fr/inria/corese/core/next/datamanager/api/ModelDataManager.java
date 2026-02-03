package fr.inria.corese.core.next.datamanager.api;

import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.datamanager.api.lifecycle.DataManagerLifecycle;
import fr.inria.corese.core.next.datamanager.api.transaction.TransactionManager;

/**
 * Main interface for the Model DataManager.
 */
public interface ModelDataManager extends ModelDataManagerRead, ModelDataManagerUpdate {

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