package fr.inria.corese.core.next.storage.api;

import fr.inria.corese.core.next.storage.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.storage.api.lifecycle.StorageLifecycle;
import fr.inria.corese.core.next.storage.api.operations.MetadataOperations;
import fr.inria.corese.core.next.storage.api.operations.MutationOperations;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;
import fr.inria.corese.core.next.storage.api.transaction.TransactionManager;

/**
 * Service-provider contract (SPI) for an RDF storage backend in Corese v5.
 *
 * <p>Repository users normally interact with the query engine or repository facade rather than
 * this low-level SPI. Storage implementations expose focused, cohesive capabilities for reads
 * ({@link #queries()}), writes ({@link #mutations()}), structural introspection ({@link #metadata()}),
 * transactions ({@link #transactions()}), and lifecycle management ({@link #lifecycle()}).</p>
 *
 * <h2>Threading &amp; Session Confinement</h2>
 * <ul>
 *   <li><strong>Root Manager:</strong> The root {@link StorageManager} instance represents the shared
 *       database state and its lifecycle. It is thread-safe for opening connection-scoped sessions.</li>
 *   <li><strong>Working Sessions:</strong> Working sessions obtained via {@link #openSession()} and
 *       their associated operation facets ({@link #queries()}, {@link #mutations()}) are confined
 *       to a single thread. Multiple threads must each obtain their own session.</li>
 *   <li><strong>Snapshot Views:</strong> Independent, point-in-time read views obtained via
 *       {@link #openReadSnapshot()} provide immutable snapshots. Reads are confined to the calling
 *       thread, but {@link #close()} must be idempotent and safe to invoke concurrently from a cancellation thread.</li>
 * </ul>
 */
public interface StorageManager extends AutoCloseable {

    /**
     * Returns a connection-scoped view sharing committed data and lifecycle.
     *
     * <p>Transactional backends override this to isolate concurrent connections,
     * including connections used on the same thread. The repository owns the lifecycle.</p>
     *
     * @return a connection-scoped {@link StorageManager} session
     */
    default StorageManager openSession() {
        return this;
    }

    /**
     * Captures an independent, read-only session over the currently visible data,
     * including this session's uncommitted writes. Later writes, commits and rollbacks
     * must not alter the captured view. The caller must close it to release snapshots.
     * The returned session belongs to the calling thread; closing it never commits
     * or rolls back the source session's transaction. Reads are confined to the
     * creating thread, but close must be idempotent and safe from a cancellation
     * thread. It must not shut down the shared database.
     *
     * @return an owned read snapshot
     * @throws UnsupportedOperationException if independent read snapshots are unavailable
     */
    default StorageManager openReadSnapshot() {
        throw new UnsupportedOperationException("Independent read snapshots are unavailable");
    }

    /** @return statement query operations */
    QueryOperations queries();

    /** @return statement mutation operations */
    MutationOperations mutations();

    /** @return storage metadata operations */
    MetadataOperations metadata();

    /**
     * Returns the transaction manager for this storage.
     *
     * @return the {@link TransactionManager}
     */
    TransactionManager transactions();

    /**
     * Returns the lifecycle manager for this storage.
     *
     * @return the {@link StorageLifecycle}
     */
    StorageLifecycle lifecycle();

    /** @return whether this backend is initialized and ready */
    default boolean isOpen() {
        return lifecycle().getState() == LifecycleState.RUNNING;
    }

    /**
     * Shuts down this backend when it is running. Closing an uninitialized or
     * already-closed backend is a no-op.
     */
    @Override
    default void close() {
        if (isOpen()) {
            lifecycle().shutdown();
        }
    }
}
