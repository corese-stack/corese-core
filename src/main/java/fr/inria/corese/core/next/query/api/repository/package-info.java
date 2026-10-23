/**
 * Repository and connection contracts for executing SPARQL operations in Corese v5.
 *
 * <h2>Lifecycle and Thread-Safety Contracts</h2>
 * <ul>
 *   <li>{@link fr.inria.corese.core.next.query.api.repository.Repository}: Long-lived, thread-safe root container
 *       owning the underlying storage backend. Closing a repository is idempotent, terminates all active sessions,
 *       and releases storage resources.</li>
 *   <li>{@link fr.inria.corese.core.next.query.api.repository.RepositoryConnection}: Lightweight, mutable session
 *       confining transactions, dataset scopes, and query preparation to a single thread. Connections are
 *       not safe for concurrent access across multiple threads.</li>
 * </ul>
 *
 * <h2>Convenience Shortcuts vs. Prepared Operations</h2>
 * <p>Convenience methods on {@code Repository} (such as {@link fr.inria.corese.core.next.query.api.repository.Repository#select(String)},
 * {@link fr.inria.corese.core.next.query.api.repository.Repository#ask(String)},
 * {@link fr.inria.corese.core.next.query.api.repository.Repository#construct(String)},
 * and {@link fr.inria.corese.core.next.query.api.repository.Repository#size})
 * manage their own connection lifecycle and return fully materialized, detached results safe for use across threads.</p>
 *
 * <p>For high-throughput, streaming, or parameterized query execution, obtain an explicit
 * {@link fr.inria.corese.core.next.query.api.repository.RepositoryConnection} via {@code getConnection()},
 * prepare operations, and consume results progressively within a {@code try-with-resources} block.</p>
 */
package fr.inria.corese.core.next.query.api.repository;
