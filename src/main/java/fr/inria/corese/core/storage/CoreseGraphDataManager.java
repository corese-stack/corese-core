package fr.inria.corese.core.storage;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.producer.MetadataManager;
import fr.inria.corese.core.storage.api.datamanager.DataManager;
import fr.inria.corese.core.storage.api.datamanager.lifecycle.DataManagerLifecycle;
import fr.inria.corese.core.storage.api.datamanager.operations.BulkOperations;
import fr.inria.corese.core.storage.api.datamanager.operations.MetadataOperations;
import fr.inria.corese.core.storage.api.datamanager.operations.MutationOperations;
import fr.inria.corese.core.storage.api.datamanager.operations.QueryOperations;
import fr.inria.corese.core.storage.api.datamanager.transaction.IsolationLevel;
import fr.inria.corese.core.storage.api.datamanager.transaction.TransactionManager;
import fr.inria.corese.core.storage.impl.lifecycle.LifecycleManagerImpl;
import fr.inria.corese.core.storage.impl.operations.BulkOperationsImpl;
import fr.inria.corese.core.storage.impl.operations.MetadataOperationsImpl;
import fr.inria.corese.core.storage.impl.operations.MutationOperationsImpl;
import fr.inria.corese.core.storage.impl.operations.QueryOperationsImpl;
import fr.inria.corese.core.storage.impl.transaction.TransactionManagerImpl;

/**
 * DataManager for corese Graph for testing purpose
 */
public class CoreseGraphDataManager implements DataManager {

    private final Graph graph;
    private final LifecycleManagerImpl lifecycleManager;
    private final QueryOperationsImpl queryOperations;
    private final MetadataOperationsImpl metadataOperations;
    private final MutationOperationsImpl mutationOperations;
    private final BulkOperationsImpl bulkOperations;
    private final TransactionManagerImpl transactionManager;
    private MetadataManager metadataManager;

    /**
     * Protected constructor.
     * Please use CoreseGraphDataManagerBuilder to create an instance.
     */
    protected CoreseGraphDataManager() {
        this(new Graph());
    }

    /**
     * Protected constructor with existing graph.
     * Please use CoreseGraphDataManagerBuilder to create an instance.
     *
     * @param graph Existing graph instance
     */
    protected CoreseGraphDataManager(Graph graph) {
        this.graph = graph;
        this.lifecycleManager = new LifecycleManagerImpl(this.graph);
        this.queryOperations = new QueryOperationsImpl(this.graph);
        this.metadataOperations = new MetadataOperationsImpl(this.graph);
        this.mutationOperations = new MutationOperationsImpl(this.graph);
        this.bulkOperations = new BulkOperationsImpl(this.graph);
        this.transactionManager = new TransactionManagerImpl(
                this.graph,
                false,
                IsolationLevel.READ_COMMITTED
        );
    }
    /**
     * Returns the transaction manager.
     *
     * @return Transaction manager
     */
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }
    /**
     * Returns the lifecycle manager.
     *
     * @return Lifecycle manager
     */
    public DataManagerLifecycle getLifecycle() {
        return lifecycleManager;
    }


    /**
     * Returns the query operations.
     *
     * @return Query operations
     */
    public QueryOperations getQueryOperations() {
        return queryOperations;
    }

    /**
     * Returns the metadata operations.
     *
     * @return Metadata operations
     */
    public MetadataOperations getMetadataOperations() {
        return metadataOperations;
    }

    /**
     * Returns the mutation operations.
     *
     * @return Mutation operations
     */
    public MutationOperations getMutationOperations() {
        return mutationOperations;
    }

    /**
     * Returns the bulk operations.
     *
     * @return Bulk operations
     */
    public BulkOperations getBulkOperations() {
        return bulkOperations;
    }

    /**
     * Returns the underlying Graph.
     * Direct graph access for advanced use cases.
     *
     * @return The graph
     */
    public Graph getGraph() {
        return graph;
    }

    /**
     * Checks if this DataManager has a MetadataManager.
     *
     * @return true if MetadataManager is set
     */
    public boolean hasMetadataManager() {
        return metadataManager != null;
    }

    /**
     * Gets the MetadataManager.
     *
     * @return MetadataManager or null
     */
    public MetadataManager getMetadataManager() {
        return metadataManager;
    }

    /**
     * Sets the MetadataManager.
     *
     * @param metadataManager MetadataManager to set
     */
    public void setMetadataManager(MetadataManager metadataManager) {
        this.metadataManager = metadataManager;
    }
}