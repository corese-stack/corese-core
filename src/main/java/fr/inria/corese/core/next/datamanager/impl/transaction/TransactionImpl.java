package fr.inria.corese.core.next.datamanager.impl.transaction;

import fr.inria.corese.core.next.datamanager.api.support.exception.DataManagerException;
import fr.inria.corese.core.next.datamanager.api.support.exception.ErrorCode;
import fr.inria.corese.core.next.datamanager.api.transaction.IsolationLevel;
import fr.inria.corese.core.next.datamanager.api.transaction.Transaction;
import fr.inria.corese.core.next.datamanager.api.transaction.TransactionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of a transaction handle for Model operations.
 */
public class TransactionImpl implements Transaction {

    private static final Logger logger = LoggerFactory.getLogger(TransactionImpl.class);

    private final String id;
    private final IsolationLevel isolationLevel;
    private final Instant startTime;
    private final TransactionCallback callback;

    /**
     * Thread-safe reference to the current state of the transaction.
     */
    private final AtomicReference<TransactionState> state;

    /**
     * Callback interface for transaction operations.
     * Allows the TransactionManager or DataManager to be notified of lifecycle changes.
     */
    public interface TransactionCallback {
        /**
         * Invoked when the transaction is requested to commit.
         *
         * @param transaction the transaction being committed.
         * @throws DataManagerException if the commit operation fails.
         */
        void onCommit(TransactionImpl transaction) throws DataManagerException;

        /**
         * Invoked when the transaction is requested to rollback.
         *
         * @param transaction the transaction being rolled back.
         * @throws DataManagerException if the rollback operation fails.
         */
        void onRollback(TransactionImpl transaction) throws DataManagerException;
    }

    /**
     * Constructs a new transaction handle.
     *
     * @param isolationLevel the requested isolation level for this transaction.
     * @param callback       the callback to handle persistence of changes.
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

    /**
     * Commits the changes made during this transaction.
     *
     * @throws IllegalStateException if the transaction is not in an ACTIVE state.
     */
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
            // Execute the commit logic via the registered callback
            callback.onCommit(this);

            // Atomically update state to COMMITTED
            if (!state.compareAndSet(TransactionState.ACTIVE, TransactionState.COMMITTED)) {
                throw new IllegalStateException(
                        "Transaction state changed during commit process"
                );
            }

            logger.info("Transaction {} committed successfully", id);

        } catch (DataManagerException e) {
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

    /**
     * Reverts the changes made during this transaction.
     *
     * @throws IllegalStateException if the transaction is not in an ACTIVE state.
     * @throws DataManagerException  if the rollback operation fails.
     */
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
            // Execute the rollback logic via the registered callback
            callback.onRollback(this);

            // Atomically update state to ROLLED_BACK
            if (!state.compareAndSet(TransactionState.ACTIVE, TransactionState.ROLLED_BACK)) {
                throw new IllegalStateException(
                        "Transaction state changed during rollback process"
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

    /**
     * Closes the transaction. If the transaction is still active, an automatic rollback is performed.
     *
     * @throws DataManagerException if an automatic rollback fails.
     */
    @Override
    public void close() throws DataManagerException {
        TransactionState currentState = state.get();

        if (currentState.isActive()) {
            logger.warn("Transaction {} closed while still active; performing automatic rollback", id);
            try {
                rollback();
            } catch (DataManagerException e) {
                logger.error("Failed to perform auto-rollback for transaction {}", id, e);
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