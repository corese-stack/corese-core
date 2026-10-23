/**
 * Native in-memory RDF storage backend with insertion-ordered SPO, POS, and OSP indexes.
 *
 * <h2>Internal Architecture</h2>
 * <ul>
 *   <li>{@link fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager}: Facade implementing
 *       the SPI {@link fr.inria.corese.core.next.storage.api.StorageManager}, coordinating query, mutation,
 *       metadata, and transaction delegates.</li>
 *   <li>{@link fr.inria.corese.core.next.storage.impl.memory.IndexedStatementStore}: Core engine managing
 *       partition-based indexing, snapshot lifecycle, and transaction commits.</li>
 *   <li>{@link fr.inria.corese.core.next.storage.impl.memory.TripleIndex}: Indexing container
 *       organizing statements in primary (SPO) and secondary (POS, OSP) orders with fast prefix iteration.</li>
 *   <li>{@link fr.inria.corese.core.next.storage.impl.memory.DenseTermTable} &amp;
 *       {@link fr.inria.corese.core.next.storage.impl.memory.CompactTermMap}: Memory-efficient hash and
 *       array-backed dictionary structures avoiding standard {@code java.util.HashMap} object overhead.</li>
 * </ul>
 *
 * <h2>Copy-On-Write (COW) Concurrency Model</h2>
 * <p>Graph partitions share immutable RDF terms and statements. Transactions and lazy reads retain
 * partition versions without deep copies; writers detach shared index containers before mutation
 * (Copy-On-Write). Ownership changes and publication use a database monitor, while sequential snapshot
 * traversal runs outside it. This does not make a single transaction or stream safe for concurrent
 * use by multiple consumers.</p>
 *
 * <h2>Stream and Resource Lifecycle</h2>
 * <p>Callers must close streams returned by query operations, preferably with {@code try-with-resources},
 * even after a short-circuiting terminal operation (such as {@code findAny()} or {@code limit()}).
 * Completion of a terminal operation alone does not guarantee release of the captured snapshot partition state.
 * Empty named graphs survive {@code CLEAR} and disappear on {@code DROP}. Batch ingestion reads its source once
 * and publishes atomically after staging and index construction.</p>
 *
 * <p>This is the default in-memory backend selected by {@code Storages.create()} or through
 * the {@code "memory"} storage configuration.</p>
 */
package fr.inria.corese.core.next.storage.impl.memory;
