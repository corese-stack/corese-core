package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.lifecycle.StorageLifecycle;
import fr.inria.corese.core.next.storage.api.operations.MetadataOperations;
import fr.inria.corese.core.next.storage.api.operations.MutationOperations;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;
import fr.inria.corese.core.next.storage.api.transaction.TransactionManager;

import java.util.Objects;

/**
 * In-memory {@link StorageManager} implementation for testing and small to
 * medium datasets.
 *
 * <p>Transactions are local to the thread that starts them and use a private
 * snapshot. A connection must not begin a transaction on one thread and
 * commit it on another. Snapshot memory usage grows with the number of stored
 * statements, so a persistent MVCC backend is preferable for very large
 * datasets.</p>
 */
public final class MemoryStorageManager implements StorageManager {

    private final InMemoryStatementStore adapter;
    private final QueryOperations queryOps;
    private final MutationOperations mutationOps;
    private final MetadataOperations metadataOps;
    private final TransactionManager txManager;
    private final StorageLifecycle lifecycle;

    /**
     * Constructs a new MemoryStorageManager.
     *
     * @param builder the builder with configuration
     */
    private MemoryStorageManager(Builder builder) {
        Objects.requireNonNull(builder, "Builder cannot be null");
        this.adapter = new InMemoryStatementStore();
        this.queryOps = new MemoryQueryOperations(adapter);
        this.mutationOps = new MemoryMutationOperations(adapter);
        this.metadataOps = new MemoryMetadataOperations(adapter);
        this.txManager = new MemoryTransactionManager(adapter);
        this.lifecycle = new MemoryLifecycleManager(adapter);
    }

    private MemoryStorageManager(InMemoryStatementStore adapter, StorageLifecycle lifecycle) {
        this.adapter = adapter;
        this.queryOps = new MemoryQueryOperations(adapter);
        this.mutationOps = new MemoryMutationOperations(adapter);
        this.metadataOps = new MemoryMetadataOperations(adapter);
        this.txManager = new MemoryTransactionManager(adapter);
        this.lifecycle = lifecycle;
    }

    @Override
    public StorageManager openSession() {
        return new MemoryStorageManager(adapter.openSession(), lifecycle);
    }

    @Override
    public QueryOperations queries() {
        return queryOps;
    }

    @Override
    public MutationOperations mutations() {
        return mutationOps;
    }

    @Override
    public MetadataOperations metadata() {
        return metadataOps;
    }

    @Override
    public TransactionManager transactions() {
        return txManager;
    }

    @Override
    public StorageLifecycle lifecycle() {
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
    public static final class Builder {


        public Builder() {
            // No configuration needed for MemoryStorageManager
        }

        /**
         * Builds a new MemoryStorageManager instance.
         *
         * @return a new MemoryStorageManager
         */
        public MemoryStorageManager build() {
            return new MemoryStorageManager(this);
        }
    }

    @Override
    public String toString() {
        return "MemoryStorageManager{size=" + adapter.size() + "}";
    }
}
