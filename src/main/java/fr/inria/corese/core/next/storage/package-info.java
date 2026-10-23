/**
 * Backend-neutral RDF storage architecture for Corese v5.
 *
 * <h2>Architecture Overview</h2>
 * <p>The {@code storage} module decouples low-level RDF statement persistence and indexing
 * from higher-level query evaluation, reasoning, and I/O. It exposes focused SPI contracts
 * that allow pluggable storage backends (in-memory, persistent, or federated).</p>
 *
 * <h2>Package Organization</h2>
 * <ul>
 *   <li>{@link fr.inria.corese.core.next.storage.api}: Public service-provider interfaces (SPI)
 *       defining storage management, queries, mutations, transactions, metadata, and lifecycle.</li>
 *   <li>{@link fr.inria.corese.core.next.storage.impl.memory}: High-performance in-memory
 *       storage backend leveraging Copy-On-Write (COW) indexing, compact dictionary encoding,
 *       and partition-based snapshot isolation.</li>
 *   <li>{@link fr.inria.corese.core.next.storage.Storages}: Static factory and service locator
 *       for creating and configuring storage managers.</li>
 *   <li>{@link fr.inria.corese.core.next.storage.StorageModels}: Bridge exposing a storage backend
 *       as a mutable, live RDF {@link fr.inria.corese.core.next.data.api.model.Model}.</li>
 * </ul>
 *
 * <h2>Threading and Concurrency Model</h2>
 * <ul>
 *   <li><strong>Shared Database vs. Sessions:</strong> The root {@link fr.inria.corese.core.next.storage.api.StorageManager}
 *       instance represents the shared database lifecycle and is thread-safe. Working sessions
 *       obtained via {@link fr.inria.corese.core.next.storage.api.StorageManager#openSession()} are
 *       confined to a single client thread.</li>
 *   <li><strong>Snapshot Isolation:</strong> Independent read views obtained via
 *       {@link fr.inria.corese.core.next.storage.api.StorageManager#openReadSnapshot()} provide immutable,
 *       point-in-time snapshots unaffected by concurrent or subsequent writes.</li>
 * </ul>
 */
package fr.inria.corese.core.next.storage;
