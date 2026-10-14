package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.lifecycle.StorageLifecycle;
import fr.inria.corese.core.next.storage.api.operations.MetadataOperations;
import fr.inria.corese.core.next.storage.api.operations.MutationOperations;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;
import fr.inria.corese.core.next.storage.api.transaction.TransactionManager;

/**
 * Native indexed memory backend. Sessions share committed data, but own transactions. Transactions and lazy reads
 * capture graph partitions, which are copied on the first mutation while shared. A read captures its version when
 * {@code find()} is called, without materializing matches. Consumers must close returned streams, including after
 * partial traversal, to release snapshot ownership.
 *
 * <p>
 * Closing a session rolls back its active transaction. Closing the owning manager also shuts down the database. Already
 * captured read streams retain their own partition ownership until they are closed or exhausted.
 * </p>
 */
public final class MemoryStorageManager implements StorageManager {
    /**
     * Session-scoped operation facade.
     */
    private final IndexedStatementStore store;
    /**
     * Owning manager flag or transaction thread, according to the enclosing class.
     */
    private final boolean owner;

    /**
     * Creates and initializes an owned backend. Normally use Storages.create(config).
     *
     * @param config storage configuration selecting this backend
     */
    public MemoryStorageManager(final StorageConfig config) {
        store = new IndexedStatementStore();
        owner = true;
        store.lifecycle().initialize(config);
    }

    /**
     * Creates a non-owning session facade sharing the supplied operation store.
     *
     * @param store session-scoped operations sharing the owning database
     */
    private MemoryStorageManager(final IndexedStatementStore store) {
        this.store = store;
        owner = false;
    }

    /**
     * Builder for MemoryStorageManager instances.
     */
    public static final class Builder {
        private StorageConfig config = StorageConfig.memory();

        public Builder config(StorageConfig config) {
            this.config = config;
            return this;
        }

        public MemoryStorageManager build() {
            return new MemoryStorageManager(config);
        }
    }

    /**
     * Creates a builder for MemoryStorageManager.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@inheritDoc}
     *
     * @return a new session sharing the database
     */
    @Override
    public StorageManager openSession() {
        return new MemoryStorageManager(store.openSession());
    }

    /**
     * {@inheritDoc}
     *
     * @return the session query operations
     */
    @Override
    public QueryOperations queries() {
        return store;
    }

    /**
     * {@inheritDoc}
     *
     * @return the session mutation operations
     */
    @Override
    public MutationOperations mutations() {
        return store;
    }

    /**
     * {@inheritDoc}
     *
     * @return the session metadata operations
     */
    @Override
    public MetadataOperations metadata() {
        return store;
    }

    /**
     * {@inheritDoc}
     *
     * @return the session transaction controller
     */
    @Override
    public TransactionManager transactions() {
        return store;
    }

    /**
     * {@inheritDoc}
     *
     * @return the shared database lifecycle controller
     */
    @Override
    public StorageLifecycle lifecycle() {
        return store.lifecycle();
    }

    /**
     * {@inheritDoc}
     *
     * @return true when the session is open and the database is running
     */
    @Override
    public boolean isOpen() {
        return store.isOpen();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() {
        store.closeSession();
        if (owner && lifecycle().getState() == fr.inria.corese.core.next.storage.api.lifecycle.LifecycleState.RUNNING) {
            lifecycle().shutdown();
        }
    }
}
