package fr.inria.corese.core.next.datamanager.impl;

import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.datamanager.api.ModelDataManager;
import fr.inria.corese.core.next.datamanager.api.ModelDataManagerBuilder;
import fr.inria.corese.core.next.datamanager.api.transaction.IsolationLevel;

/**
 * Builder implementation for creating CoreseModelDataManager instances.
 * Provides a fluent API for configuring the DataManager.
 */
public class CoreseModelDataManagerBuilder implements ModelDataManagerBuilder {

    private Model model;
    private boolean transactionSupport = false;
    private IsolationLevel defaultIsolationLevel = IsolationLevel.READ_COMMITTED;

    /**
     * Creates a new CoreseModelDataManagerBuilder.
     */
    public CoreseModelDataManagerBuilder() {
    }

    /**
     * Sets the Corese Model instance to use.
     *
     * @param model the Model instance
     * @return this builder
     */
    public CoreseModelDataManagerBuilder model(Model model) {
        this.model = model;
        return this;
    }

    /**
     * Enables or disables transaction support.
     *
     * @param enable true to enable transactions
     * @return this builder
     */
    public CoreseModelDataManagerBuilder withTransactions(boolean enable) {
        this.transactionSupport = enable;
        return this;
    }

    /**
     * Sets the default isolation level for transactions.
     *
     * @param level the isolation level
     * @return this builder
     */
    public CoreseModelDataManagerBuilder defaultIsolationLevel(IsolationLevel level) {
        this.defaultIsolationLevel = level;
        return this;
    }

    /**
     * Builds the CoreseModelDataManager with the configured settings.
     *
     * @return a new CoreseModelDataManager instance
     * @throws IllegalStateException if no model has been provided
     */
    @Override
    public ModelDataManager build() {
        if (model == null) {
            throw new IllegalStateException("Model must be provided before building");
        }

        return CoreseModelDataManager.builder()
                .model(model)
                .transactionSupport(transactionSupport)
                .defaultIsolationLevel(defaultIsolationLevel)
                .build();
    }
}