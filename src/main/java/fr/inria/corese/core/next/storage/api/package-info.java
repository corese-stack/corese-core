/**
 * Service-provider contracts (SPI) for pluggable RDF storage backends in Corese v5.
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link fr.inria.corese.core.next.storage.api.StorageManager}: Central coordinator for a storage
 *       instance, exposing read, write, metadata, transaction, and lifecycle facets.</li>
 *   <li>{@link fr.inria.corese.core.next.storage.api.operations.QueryOperations}: High-performance pattern
 *       matching and statement streaming.</li>
 *   <li>{@link fr.inria.corese.core.next.storage.api.operations.MutationOperations}: Statement additions,
 *       deletions, batch operations, and named graph registry management.</li>
 *   <li>{@link fr.inria.corese.core.next.storage.api.operations.MetadataOperations}: Introspection of
 *       predicates, subjects, objects, named contexts, and cardinality statistics for cost-based query optimization.</li>
 *   <li>{@link fr.inria.corese.core.next.storage.api.transaction.TransactionManager}: ACID / Snapshot transaction
 *       boundaries and isolation control.</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 * <p>Contracts in this package follow the Single Responsibility Principle (SRP) by segregating
 * query, mutation, introspection, and transaction capabilities into distinct operation interfaces.
 * Implementations are expected to be backend-neutral, hiding internal indexing or memory layout details.</p>
 */
package fr.inria.corese.core.next.storage.api;
