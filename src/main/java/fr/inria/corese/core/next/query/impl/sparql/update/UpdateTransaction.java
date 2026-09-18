package fr.inria.corese.core.next.query.impl.sparql.update;

import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.transaction.Transaction;

/** Request-level commit/rollback, including a savepoint fallback for legacy storage. */
public final class UpdateTransaction {
    private UpdateTransaction() { }

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
