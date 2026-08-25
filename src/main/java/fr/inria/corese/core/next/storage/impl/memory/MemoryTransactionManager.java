package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.storage.api.transaction.IsolationLevel;
import fr.inria.corese.core.next.storage.api.transaction.Transaction;
import fr.inria.corese.core.next.storage.api.transaction.TransactionManager;

import java.util.Collections;
import java.util.Set;

/**
 * transaction manager for {@link MemoryStorageManager}.
 */
final class MemoryTransactionManager implements TransactionManager {

    /**
     * Always returns false as MemoryStorageManager does not support transactions.
     *
     * @return false
     */
    @Override
    public boolean supportsTransactions() {
        return false;
    }

    /**
     * Attempts to begin a transaction.
     *
     * @return never returns (always throws)
     */
    @Override
    public Transaction beginTransaction() {
        throw new UnsupportedOperationException("Transactions not supported by MemoryStorageManager");
    }

    /**
     * Attempts to begin a transaction with a specific isolation level.
     *
     * @param level the requested isolation level (ignored)
     * @return never returns (always throws)
     */
    @Override
    public Transaction beginTransaction(IsolationLevel level) {
        throw new UnsupportedOperationException("Transactions not supported by MemoryStorageManager");
    }

    /**
     * Returns the set of supported isolation levels.
     *
     * @return empty set
     */
    @Override
    public Set<IsolationLevel> getSupportedIsolationLevels() {
        return Collections.emptySet();
    }
}
