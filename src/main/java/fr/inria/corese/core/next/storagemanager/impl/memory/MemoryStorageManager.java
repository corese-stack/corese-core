package fr.inria.corese.core.next.storagemanager.impl.memory;

import fr.inria.corese.core.next.storagemanager.api.StorageManager;
import fr.inria.corese.core.next.storagemanager.api.StorageManagerBuilder;
import fr.inria.corese.core.next.storagemanager.api.lifecycle.StorageLifecycle;
import fr.inria.corese.core.next.storagemanager.api.operations.BulkOperations;
import fr.inria.corese.core.next.storagemanager.api.operations.MetadataOperations;
import fr.inria.corese.core.next.storagemanager.api.operations.MutationOperations;
import fr.inria.corese.core.next.storagemanager.api.operations.QueryOperations;
import fr.inria.corese.core.next.storagemanager.api.transaction.TransactionManager;

/**
 * In-memory {@link StorageManager} implementation for testing and small datasets.
 */
public class MemoryStorageManager implements StorageManager {

    private final InMemoryStatementStore adapter;
    private final QueryOperations queryOps;
    private final MutationOperations mutationOps;
    private final MetadataOperations metadataOps;
    private final BulkOperations bulkOps;
    private final TransactionManager txManager;
    private final StorageLifecycle lifecycle;

    /**
     * Constructs a new MemoryStorageManager.
     *
     * @param builder the builder with configuration
     */
    protected MemoryStorageManager(Builder builder) {
        this.adapter = new InMemoryStatementStore();
        this.queryOps = new MemoryQueryOperations(adapter);
        this.mutationOps = new MemoryMutationOperations(adapter);
        this.metadataOps = new MemoryMetadataOperations(adapter);
        this.bulkOps = new MemoryBulkOperations(adapter);
        this.txManager = new MemoryTransactionManager();
        this.lifecycle = new MemoryLifecycleManager(adapter);
    }

    @Override
    public QueryOperations getQueryOperations() {
        return queryOps;
    }

    @Override
    public MutationOperations getMutationOperations() {
        return mutationOps;
    }

    @Override
    public MetadataOperations getMetadataOperations() {
        return metadataOps;
    }

    @Override
    public BulkOperations getBulkOperations() {
        return bulkOps;
    }

    @Override
    public TransactionManager getTransactionManager() {
        return txManager;
    }

    @Override
    public StorageLifecycle getLifecycle() {
        return lifecycle;
    }

    /**
     * Returns a new builder for MemoryStorageManager.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MemoryStorageManager}.
     */
    public static final class Builder implements StorageManagerBuilder {


        public Builder() {
            // No configuration needed for MemoryStorageManager
        }

        /**
         * Builds a new MemoryStorageManager instance.
         *
         * @return a new MemoryStorageManager
         */
        @Override
        public MemoryStorageManager build() {
            return new MemoryStorageManager(this);
        }
    }

    @Override
    public String toString() {
        return "MemoryStorageManager{size=" + adapter.size() + "}";
    }
}
