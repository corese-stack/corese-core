package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.storage.api.exception.ErrorCode;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.transaction.Transaction;
import fr.inria.corese.core.next.storage.api.transaction.TransactionState;

import java.util.Objects;
import java.util.UUID;

/**
 * Thread-owned optimistic transaction retaining a captured state until commit or rollback.
 */
final class SnapshotTransaction implements Transaction {

    private final String id = UUID.randomUUID().toString();
    private final Thread owner = Thread.currentThread();
    private final MemoryDatabase database;
    private final IndexedStatementStore session;
    private final long version;
    long revision;
    final boolean readOnly;
    PartitionState state;
    boolean dirty;
    private TransactionState status = TransactionState.ACTIVE;

    SnapshotTransaction(final MemoryDatabase database, final IndexedStatementStore session, final PartitionState state,
            final long version) {
        this(database, session, state, version, false);
    }

    SnapshotTransaction(final MemoryDatabase database, final IndexedStatementStore session, final PartitionState state,
            final long version, final boolean readOnly) {
        this.database = Objects.requireNonNull(database, "database");
        this.session = Objects.requireNonNull(session, "session");
        this.state = Objects.requireNonNull(state, "state");
        this.version = version;
        this.revision = version;
        this.readOnly = readOnly;
    }

    void checkOwner() {
        if (Thread.currentThread() != owner) {
            throw new IllegalStateException("Transaction belongs to another thread");
        }
    }

    private void requireActive() {
        checkOwner();
        if (status != TransactionState.ACTIVE) {
            throw new IllegalStateException("Transaction is inactive");
        }
        if (session.isLoading()) {
            throw new IllegalStateException("A batch is active on this session");
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isActive() {
        synchronized (database) {
            return status == TransactionState.ACTIVE;
        }
    }

    @Override
    public TransactionState getState() {
        synchronized (database) {
            return status;
        }
    }

    @Override
    public void commit() {
        synchronized (database) {
            requireActive();
            session.checkAccess();
            if (database.version != version) {
                throw new StorageException(ErrorCode.TRANSACTION_ERROR,
                        "Concurrent storage modification; roll back and retry");
            }
            if (dirty) {
                database.committed.release();
                database.committed = state;
                state = null; // Ownership transferred to the committed state.
                database.version = revision;
            }
            finish(TransactionState.COMMITTED);
        }
    }

    @Override
    public void rollback() {
        synchronized (database) {
            requireActive();
            finish(TransactionState.ROLLED_BACK);
        }
    }

    void finish(final TransactionState terminal) {
        status = terminal;
        if (state != null) {
            state.release();
        }
        state = null;
        session.clearTransaction();
    }

    @Override
    public void close() {
        synchronized (database) {
            if (isActive()) {
                rollback();
            }
        }
    }
}
