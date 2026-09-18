package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.storage.api.transaction.IsolationLevel;
import fr.inria.corese.core.next.storage.api.transaction.Transaction;
import fr.inria.corese.core.next.storage.api.transaction.TransactionManager;
import fr.inria.corese.core.next.storage.api.transaction.TransactionState;

import java.util.Set;
import java.util.UUID;

/** Optimistic serializable transactions with private snapshots and conflict detection. */
final class MemoryTransactionManager implements TransactionManager {
    private final InMemoryStatementStore store;

    MemoryTransactionManager(InMemoryStatementStore store) {
        this.store = store;
    }

    @Override
    public boolean supportsTransactions() {
        return true;
    }

    @Override
    public Transaction beginTransaction() {
        return beginTransaction(IsolationLevel.SERIALIZABLE);
    }

    @Override
    public Transaction beginTransaction(IsolationLevel level) {
        if (!getSupportedIsolationLevels().contains(level)) {
            throw new IllegalArgumentException("Unsupported isolation level: " + level);
        }
        return new SnapshotTransaction(store.begin());
    }

    @Override
    public Set<IsolationLevel> getSupportedIsolationLevels() {
        return Set.of(IsolationLevel.SERIALIZABLE);
    }

    private final class SnapshotTransaction implements Transaction {
        private final String id = UUID.randomUUID().toString();
        private final Thread owner = Thread.currentThread();
        private final long version;
        private TransactionState state = TransactionState.ACTIVE;

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
