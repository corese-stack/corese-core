package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.storage.api.transaction.IsolationLevel;
import fr.inria.corese.core.next.storage.api.transaction.Transaction;
import fr.inria.corese.core.next.storage.api.transaction.TransactionManager;
import fr.inria.corese.core.next.storage.api.transaction.TransactionState;

import java.util.Set;
import java.util.UUID;

/**
 * Transaction manager for {@link MemoryStorageManager}.
 *
 * <p>Provides optimistic serializable transactions with private snapshots and conflict detection.</p>
 */
final class MemoryTransactionManager implements TransactionManager {
    private final InMemoryStatementStore store;

    /**
     * Constructs a new transaction manager for the specified memory store.
     *
     * @param store the underlying statement store
     */
    MemoryTransactionManager(InMemoryStatementStore store) {
        this.store = store;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} as in-memory storage supports transactions
     */
    @Override
    public boolean supportsTransactions() {
        return true;
    }

    /**
     * Begins a new transaction with default isolation level ({@link IsolationLevel#SERIALIZABLE}).
     *
     * @return the created transaction handle
     */
    @Override
    public Transaction beginTransaction() {
        return beginTransaction(IsolationLevel.SERIALIZABLE);
    }

    /**
     * Begins a new transaction with the specified isolation level.
     *
     * @param level the desired isolation level
     * @return the created transaction handle
     * @throws IllegalArgumentException if {@code level} is not supported
     */
    @Override
    public Transaction beginTransaction(IsolationLevel level) {
        if (!getSupportedIsolationLevels().contains(level)) {
            throw new IllegalArgumentException("Unsupported isolation level: " + level);
        }
        return new SnapshotTransaction(store.begin());
    }

    /**
     * Returns the set of isolation levels supported by this manager.
     *
     * @return set containing {@link IsolationLevel#SERIALIZABLE}
     */
    @Override
    public Set<IsolationLevel> getSupportedIsolationLevels() {
        return Set.of(IsolationLevel.SERIALIZABLE);
    }

    /**
     * Snapshot-isolated transaction with thread-affinity and conflict detection.
     */
    private final class SnapshotTransaction implements Transaction {
        private final String id = UUID.randomUUID().toString();
        private final Thread owner = Thread.currentThread();
        private final long version;
        private TransactionState state = TransactionState.ACTIVE;

        /**
         * Constructs a new snapshot transaction.
         *
         * @param version the database version at snapshot creation
         */
        SnapshotTransaction(long version) {
            this.version = version;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void commit() {
            requireActive();
            store.commit(version);
            state = TransactionState.COMMITTED;
        }

        @Override
        public void rollback() {
            requireActive();
            store.rollback();
            state = TransactionState.ROLLED_BACK;
        }

        private void requireActive() {
            if (Thread.currentThread() != owner || !isActive()) {
                throw new IllegalStateException("Transaction is inactive or belongs to another thread");
            }
        }

        @Override
        public boolean isActive() {
            return state == TransactionState.ACTIVE;
        }

        @Override
        public TransactionState getState() {
            return state;
        }

        @Override
        public void close() {
            if (isActive()) {
                rollback();
            }
        }
    }
}
