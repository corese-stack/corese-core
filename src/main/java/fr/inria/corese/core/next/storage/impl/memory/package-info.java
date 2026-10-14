/**
 * Native in-memory RDF storage with insertion-ordered SPO, POS and OSP indexes.
 *
 * <p>
 * Graph partitions share immutable RDF terms and statements. Transactions and lazy reads retain partition versions;
 * writers detach shared index containers before mutation. Ownership changes and publication use a database monitor,
 * while sequential snapshot traversal runs outside it. This does not make a single transaction or stream safe for
 * concurrent use by multiple consumers.
 *
 * <p>
 * Callers must close streams returned by query operations, preferably with try-with-resources, even after a
 * short-circuiting terminal operation. Completion of a terminal operation alone does not guarantee release of the
 * captured state. Empty named graphs survive CLEAR and disappear on DROP. Batch ingestion reads its source once and
 * publishes atomically after staging and index construction.
 *
 * <p>
 * This is the default in-memory backend selected by {@code Storages.create()} or through the {@code "memory"}
 * storage configuration.
 */
package fr.inria.corese.core.next.storage.impl.memory;
