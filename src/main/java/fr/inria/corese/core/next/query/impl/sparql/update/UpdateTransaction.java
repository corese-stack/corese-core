package fr.inria.corese.core.next.query.impl.sparql.update;

import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.transaction.Transaction;

/**
 * Request-level commit/rollback coordinator, including a savepoint fallback for non-transactional storage.
 */
public final class UpdateTransaction {

    private UpdateTransaction() { }

    /**
     * Executes the specified update action in a transaction if supported, or using a snapshot savepoint otherwise.
     *
     * @param storage the storage manager to run the update against
     * @param action  the update action to execute
     * @throws RuntimeException if update execution fails, after rolling back or restoring the snapshot
     */
    public static void execute(StorageManager storage, Runnable action) {
        if (storage.transactions().supportsTransactions()) {
            try (Transaction transaction = storage.transactions().beginTransaction()) {
                action.run();
                transaction.commit();
            }
        } else {
            executeWithSavepoint(storage, action);
        }
    }

    /**
     * Executes the specified update action with a snapshot savepoint restored in case of failure.
     *
     * @param storage the storage manager to capture and restore
     * @param action  the update action to execute
     * @throws RuntimeException if update execution fails, with suppressed restoration failure if any
     */
    public static void executeWithSavepoint(StorageManager storage, Runnable action) {
        UpdateSnapshot snapshot = UpdateSnapshot.capture(storage);
        try {
            action.run();
        } catch (RuntimeException failure) {
            try {
                snapshot.restore(storage);
            } catch (RuntimeException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
    }
}
