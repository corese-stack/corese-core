package fr.inria.corese.core.next.datamanager.impl;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.datamanager.api.ModelDataManager;
import fr.inria.corese.core.next.datamanager.api.lifecycle.DataManagerLifecycle;
import fr.inria.corese.core.next.datamanager.api.operations.BulkOperations;
import fr.inria.corese.core.next.datamanager.api.operations.MetadataOperations;
import fr.inria.corese.core.next.datamanager.api.operations.MutationOperations;
import fr.inria.corese.core.next.datamanager.api.operations.QueryOperations;
import fr.inria.corese.core.next.datamanager.api.transaction.IsolationLevel;
import fr.inria.corese.core.next.datamanager.api.transaction.TransactionManager;
import fr.inria.corese.core.next.datamanager.impl.lifecycle.LifecycleManagerImpl;
import fr.inria.corese.core.next.datamanager.impl.operations.BulkOperationsImpl;
import fr.inria.corese.core.next.datamanager.impl.operations.MetadataOperationsImpl;
import fr.inria.corese.core.next.datamanager.impl.operations.MutationOperationsImpl;
import fr.inria.corese.core.next.datamanager.impl.operations.QueryOperationsImpl;
import fr.inria.corese.core.next.datamanager.impl.transaction.TransactionManagerImpl;

/**
 * Implementation of ModelDataManager for Corese Model.
 * Coordinates all DataManager components and provides a unified API.
 */
public class CoreseModelDataManager implements ModelDataManager {

    private final Model model;
    private final QueryOperations queryOperations;
    private final MutationOperations mutationOperations;
    private final MetadataOperations metadataOperations;
    private final BulkOperations bulkOperations;
    private final TransactionManager transactionManager;
    private final DataManagerLifecycle lifecycle;

    /**
     * Protected constructor - use Builder to create instances.
     *
     * @param builder the builder containing configuration
     */
    protected CoreseModelDataManager(Builder builder) {
        this.model = builder.model;

        // Initialize operation handlers
        this.queryOperations = new QueryOperationsImpl(model);
        this.mutationOperations = new MutationOperationsImpl(model);
        this.metadataOperations = new MetadataOperationsImpl(model);
        this.bulkOperations = new BulkOperationsImpl(model);

        // Initialize transaction manager
        this.transactionManager = new TransactionManagerImpl(
                model,
                builder.transactionSupport,
                builder.defaultIsolationLevel
        );

        // Initialize lifecycle manager
        this.lifecycle = new LifecycleManagerImpl(model);
    }

    @Override
    public QueryOperations getQueryOperations() {
        return queryOperations;
    }

    @Override
    public MutationOperations getMutationOperations() {
        return mutationOperations;
    }

    @Override
    public MetadataOperations getMetadataOperations() {
        return metadataOperations;
    }

    @Override
    public BulkOperations getBulkOperations() {
        return bulkOperations;
    }

    @Override
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public DataManagerLifecycle getLifecycle() {
        return lifecycle;
    }

    @Override
    public Model getModel() {
        return model;
    }

    /**
     * Creates a new builder for CoreseModelDataManager.
     *
     * @return new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for CoreseModelDataManager.
     */
    public static class Builder {
        private Model model;
        private boolean transactionSupport = false;
        private IsolationLevel defaultIsolationLevel = IsolationLevel.READ_COMMITTED;

        /**
         * Sets the Corese Model instance.
         *
         * @param model the Model to use
         * @return this builder
         */
        public Builder model(Model model) {
            this.model = model;
            return this;
        }

        /**
         * Enables or disables transaction support.
         *
         * @param enable true to enable transactions
         * @return this builder
         */
        public Builder transactionSupport(boolean enable) {
            this.transactionSupport = enable;
            return this;
        }

        /**
         * Sets the default isolation level for transactions.
         *
         * @param level the isolation level
         * @return this builder
         */
        public Builder defaultIsolationLevel(IsolationLevel level) {
            this.defaultIsolationLevel = level;
            return this;
        }

        /**
         * Builds the CoreseModelDataManager instance.
         *
         * @return new CoreseModelDataManager instance
         * @throws IllegalStateException if model is not set
         */
        public CoreseModelDataManager build() {
            if (model == null) {
                throw new IllegalStateException("Model must be set before building");
            }
            return new CoreseModelDataManager(this);
        }
    }

    @Override
    public String toString() {
        return "CoreseModelDataManager{" +
                "modelSize=" + model.size() +
                ", transactionsSupported=" + transactionManager.supportsTransactions() +
                ", lifecycleState=" + lifecycle.getState() +
                '}';
    }
}