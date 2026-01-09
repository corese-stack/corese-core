package fr.inria.corese.core.storage.impl.transaction;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.storage.api.dataManager.support.exception.DataManagerException;
import fr.inria.corese.core.storage.api.dataManager.support.exception.ErrorCode;
import fr.inria.corese.core.storage.api.dataManager.transaction.IsolationLevel;
import fr.inria.corese.core.storage.api.dataManager.transaction.Transaction;
import fr.inria.corese.core.storage.api.dataManager.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Transaction manager implementation for CoreseGraphDataManager.
 */
public class TransactionManagerImpl implements TransactionManager {

    private static final Logger logger = LoggerFactory.getLogger(TransactionManagerImpl.class);

    private final Graph graph;
    private final boolean transactionSupport;
    private final IsolationLevel defaultIsolationLevel;

    // Thread-local storage for current transaction per thread
    private final ThreadLocal<TransactionImpl> currentTransaction = new ThreadLocal<>();

    /**
     * Constructs a transaction manager.
     *
     * @param graph                 Graph to manage
     * @param transactionSupport    Whether transactions are enabled
     * @param defaultIsolationLevel Default isolation level
     */
    public TransactionManagerImpl(
            Graph graph,
            boolean transactionSupport,
            IsolationLevel defaultIsolationLevel) {

        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }
        if (defaultIsolationLevel == null) {
            throw new IllegalArgumentException("Default isolation level cannot be null");
        }

        this.graph = graph;
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
            throw new UnsupportedOperationException("Transactions are not supported");
        }

        if (isolationLevel == null) {
            throw new IllegalArgumentException("Isolation level cannot be null");
        }

        if (!getSupportedIsolationLevels().contains(isolationLevel)) {
            throw new IllegalArgumentException(
                    "Isolation level " + isolationLevel + " is not supported"
            );
        }

        // Check if there's already an active transaction
        if (hasActiveTransaction()) {
            TransactionImpl existing = currentTransaction.get();
            logger.warn("Starting new transaction while transaction {} is still active",
                    existing.getId());
            // For now, we don't support nested transactions
            throw new DataManagerException(
                    ErrorCode.TRANSACTION_ERROR,
                    "Nested transactions are not supported. " +
                            "Current transaction: " + existing.getId()
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

        // Create and register transaction
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
        // For now, we support all isolation levels
        // In a real implementation, this would depend on the backend storage
        return EnumSet.allOf(IsolationLevel.class);
    }

    /**
     * Performs the actual commit operation.
     * Called by the transaction when commit() is invoked.
     *
     * @param transaction Transaction to commit
     * @throws DataManagerException if commit fails
     */
    private void performCommit(TransactionImpl transaction) throws DataManagerException {
        try {
            logger.debug("Performing commit for transaction {}", transaction.getId());

            graph.init();

            // Clear thread-local
            currentTransaction.remove();

            logger.debug("Commit completed for transaction {}", transaction.getId());

        } catch (Exception e) {
            logger.error("Failed to commit transaction {}", transaction.getId(), e);
            throw new DataManagerException(
                    ErrorCode.TRANSACTION_ERROR,
                    "Failed to commit transaction: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Performs the actual rollback operation.
     * Called by the transaction when rollback() is invoked.
     *
     * @param transaction Transaction to rollback
     * @throws DataManagerException if rollback fails
     */
    private void performRollback(TransactionImpl transaction) throws DataManagerException {
        try {
            logger.debug("Performing rollback for transaction {}", transaction.getId());

            graph.init();

            // Clear thread-local
            currentTransaction.remove();

            logger.debug("Rollback completed for transaction {}", transaction.getId());

        } catch (Exception e) {
            logger.error("Failed to rollback transaction {}", transaction.getId(), e);
            throw new DataManagerException(
                    ErrorCode.TRANSACTION_ERROR,
                    "Failed to rollback transaction: " + e.getMessage(),
                    e
            );
        }
    }

}