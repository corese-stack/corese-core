package fr.inria.corese.core.storage.impl.transaction;

import fr.inria.corese.core.storage.api.dataManager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.dataManager.support.exception.ErrorCode;
import fr.inria.corese.core.storage.api.dataManager.transaction.IsolationLevel;
import fr.inria.corese.core.storage.api.dataManager.transaction.Transaction;
import fr.inria.corese.core.storage.api.dataManager.transaction.TransactionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of a transaction handle.
 */
public class TransactionImpl implements Transaction {

    private static final Logger logger = LoggerFactory.getLogger(TransactionImpl.class);

    private final String id;
    private final IsolationLevel isolationLevel;
    private final Instant startTime;
    private final TransactionCallback callback;

    private final AtomicReference<TransactionState> state;

    /**
     * Callback interface for transaction operations.
     * Allows the TransactionManager to be notified of commits/rollbacks.
     */
    public interface TransactionCallback {
        void onCommit(TransactionImpl transaction) throws DataManagerException;

        void onRollback(TransactionImpl transaction) throws DataManagerException;
    }

    /**
     * Constructs a new transaction.
     *
     * @param isolationLevel Isolation level
     * @param callback       Callback for commit/rollback operations
     */
    public TransactionImpl(IsolationLevel isolationLevel, TransactionCallback callback) {
        this.id = UUID.randomUUID().toString();
        this.isolationLevel = isolationLevel;
        this.startTime = Instant.now();
        this.callback = callback;
        this.state = new AtomicReference<>(TransactionState.ACTIVE);

        logger.debug("Transaction {} started with isolation level {}", id, isolationLevel);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public IsolationLevel getIsolationLevel() {
        return isolationLevel;
    }


    @Override
    public void commit() throws DataManagerException {
        TransactionState currentState = state.get();

        if (!currentState.isActive()) {
            throw new IllegalStateException(
                    "Cannot commit transaction in state: " + currentState
            );
        }

        logger.debug("Committing transaction {}", id);

        try {
            // Notify callback
            callback.onCommit(this);

            // Update state
            if (!state.compareAndSet(TransactionState.ACTIVE, TransactionState.COMMITTED)) {
                throw new IllegalStateException(
                        "Transaction state changed during commit"
                );
            }

            logger.info("Transaction {} committed successfully", id);

        } catch (DataManagerException e) {
            // Mark as failed
            state.set(TransactionState.FAILED);
            logger.error("Failed to commit transaction {}", id, e);
            throw e;

        } catch (Exception e) {
            state.set(TransactionState.FAILED);
            logger.error("Unexpected error during commit of transaction {}", id, e);
            throw new DataManagerException(
                    ErrorCode.TRANSACTION_ERROR,
                    "Failed to commit transaction: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public void rollback() throws DataManagerException {
        TransactionState currentState = state.get();

        if (!currentState.isActive()) {
            throw new IllegalStateException(
                    "Cannot rollback transaction in state: " + currentState
            );
        }

        logger.debug("Rolling back transaction {}", id);

        try {
            // Notify callback
            callback.onRollback(this);

            // Update state
            if (!state.compareAndSet(TransactionState.ACTIVE, TransactionState.ROLLED_BACK)) {
                throw new IllegalStateException(
                        "Transaction state changed during rollback"
                );
            }

            logger.info("Transaction {} rolled back successfully", id);

        } catch (DataManagerException e) {
            state.set(TransactionState.FAILED);
            logger.error("Failed to rollback transaction {}", id, e);
            throw e;

        } catch (Exception e) {
            state.set(TransactionState.FAILED);
            logger.error("Unexpected error during rollback of transaction {}", id, e);
            throw new DataManagerException(
                    ErrorCode.TRANSACTION_ERROR,
                    "Failed to rollback transaction: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public boolean isActive() {
        return state.get().isActive();
    }

    @Override
    public TransactionState getState() {
        return state.get();
    }

    @Override
    public void close() throws DataManagerException {
        TransactionState currentState = state.get();

        if (currentState.isActive()) {
            logger.warn("Transaction {} not committed, performing automatic rollback", id);
            try {
                rollback();
            } catch (DataManagerException e) {
                logger.error("Failed to auto-rollback transaction {}", id, e);
                throw e;
            }
        }

        logger.debug("Transaction {} closed in state {}", id, state.get());
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + id + '\'' +
                ", isolationLevel=" + isolationLevel +
                ", state=" + state.get() +
                ", startTime=" + startTime +
                '}';
    }
}