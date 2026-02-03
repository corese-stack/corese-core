package fr.inria.corese.core.next.datamanager.impl.transaction;

import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.datamanager.api.support.exception.DataManagerException;
import fr.inria.corese.core.next.datamanager.api.support.exception.ErrorCode;
import fr.inria.corese.core.next.datamanager.api.transaction.IsolationLevel;
import fr.inria.corese.core.next.datamanager.api.transaction.Transaction;
import fr.inria.corese.core.next.datamanager.api.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Transaction manager implementation for CoreseModelDataManager.
 * Note: This is a basic implementation as Model may not support native transactions.
 */
public class TransactionManagerImpl implements TransactionManager {

    private static final Logger logger = LoggerFactory.getLogger(TransactionManagerImpl.class);

    private final boolean transactionSupport;
    private final IsolationLevel defaultIsolationLevel;

    /**
     * Thread-local storage to track the current active transaction handle for the calling thread.
     */
    private final ThreadLocal<TransactionImpl> currentTransaction = new ThreadLocal<>();

    /**
     * Constructs a new TransactionManager.
     *
     * @param model                 the underlying Corese Model.
     * @param transactionSupport    flag indicating if transaction features are enabled.
     * @param defaultIsolationLevel the isolation level to use when none is specified.
     * @throws IllegalArgumentException if model or defaultIsolationLevel is null.
     */
    public TransactionManagerImpl(
            Model model,
            boolean transactionSupport,
            IsolationLevel defaultIsolationLevel) {

        if (model == null) {
            throw new IllegalArgumentException("Model cannot be null");
        }
        if (defaultIsolationLevel == null) {
            throw new IllegalArgumentException("Default isolation level cannot be null");
        }

        this.transactionSupport = transactionSupport;
        this.defaultIsolationLevel = defaultIsolationLevel;

        logger.info("TransactionManager initialized: support={}, defaultLevel={}",
                transactionSupport, defaultIsolationLevel);
    }

    @Override
    public boolean supportsTransactions() {
        return transactionSupport;
    }

    @Override
    public Transaction beginTransaction() throws DataManagerException {
        return beginTransaction(defaultIsolationLevel);
    }

    @Override
    public Transaction beginTransaction(IsolationLevel isolationLevel) throws DataManagerException {
        if (!supportsTransactions()) {
            throw new UnsupportedOperationException("Transactions are not supported by this manager configuration.");
        }

        if (isolationLevel == null) {
            throw new IllegalArgumentException("Isolation level cannot be null");
        }

        if (!getSupportedIsolationLevels().contains(isolationLevel)) {
            throw new IllegalArgumentException(
                    "Isolation level " + isolationLevel + " is not supported by the current storage backend."
            );
        }

        // Check for existing active transaction to prevent unsupported nesting
        TransactionImpl existing = currentTransaction.get();
        if (existing != null && existing.isActive()) {
            logger.warn("Attempted to start a new transaction while transaction {} is still active.",
                    existing.getId());
            throw new DataManagerException(
                    ErrorCode.TRANSACTION_ERROR,
                    "Nested transactions are not supported. Current active transaction: " + existing.getId()
            );
        }

        TransactionImpl.TransactionCallback callback = new TransactionImpl.TransactionCallback() {
            @Override
            public void onCommit(TransactionImpl transaction) throws DataManagerException {
                performCommit(transaction);
            }

            @Override
            public void onRollback(TransactionImpl transaction) throws DataManagerException {
                performRollback(transaction);
            }
        };

        // Create the new transaction handle and register it to the current thread
        TransactionImpl transaction = new TransactionImpl(isolationLevel, callback);
        currentTransaction.set(transaction);

        logger.info("Started transaction {} with isolation level {}",
                transaction.getId(), isolationLevel);

        return transaction;
    }

    @Override
    public Optional<Transaction> getCurrentTransaction() {
        return Optional.ofNullable(currentTransaction.get());
    }

    @Override
    public IsolationLevel getDefaultIsolationLevel() {
        return defaultIsolationLevel;
    }

    @Override
    public Set<IsolationLevel> getSupportedIsolationLevels() {
        // Model currently supports standard levels
        return EnumSet.allOf(IsolationLevel.class);
    }

    /**
     * Executes the internal commit logic for a transaction.
     * This method is triggered by the {@link TransactionImpl#commit()} method via the callback.
     *
     * @param transaction the transaction to commit.
     * @throws DataManagerException if the model commit fails.
     */
    private void performCommit(TransactionImpl transaction) throws DataManagerException {
        try {
            logger.debug("Performing commit for transaction {}", transaction.getId());

            // Model doesn't have a native init() or commit() method
            // Changes are already applied to the model
            // This is a no-op for now

            // Clear the thread-local reference as the transaction lifecycle is ending
            currentTransaction.remove();

            logger.debug("Commit completed for transaction {}", transaction.getId());

        } catch (Exception e) {
            logger.error("Failed to commit transaction {}", transaction.getId(), e);
            throw new DataManagerException(
                    ErrorCode.TRANSACTION_ERROR,
                    "Failed to commit transaction changes to the model: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Executes the internal rollback logic for a transaction.
     * This method is triggered by the {@link TransactionImpl#rollback()} method via the callback.
     *
     * @param transaction the transaction to roll back.
     * @throws DataManagerException if the rollback operation fails.
     */
    private void performRollback(TransactionImpl transaction) throws DataManagerException {
        try {
            logger.debug("Performing rollback for transaction {}", transaction.getId());

            // Model doesn't support native rollback
            // In a real implementation, you would need to track changes and revert them
            // For now, this is a no-op
            logger.warn("Model does not support native transaction rollback - changes cannot be reverted");

            // Clear the thread-local reference
            currentTransaction.remove();

            logger.debug("Rollback completed for transaction {}", transaction.getId());

        } catch (Exception e) {
            logger.error("Failed to rollback transaction {}", transaction.getId(), e);
            throw new DataManagerException(
                    ErrorCode.TRANSACTION_ERROR,
                    "Failed to rollback transaction changes: " + e.getMessage(),
                    e
            );
        }
    }
}