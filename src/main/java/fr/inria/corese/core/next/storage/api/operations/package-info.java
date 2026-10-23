/**
 * Focused query, mutation, and metadata operation contracts for RDF storage backends.
 *
 * <h2>Operation Facets</h2>
 * <ul>
 *   <li>{@link fr.inria.corese.core.next.storage.api.operations.QueryOperations}: Read operations providing
 *       lazy statement streams ({@code find}), exact counts ({@code count}), fast existence tests
 *       ({@code contains}), and cache invalidation tokens ({@code getDataVersion}).</li>
 *   <li>{@link fr.inria.corese.core.next.storage.api.operations.MutationOperations}: Write operations providing
 *       single and batch additions/removals, pattern-based deletions, and graph registry lifecycle.</li>
 *   <li>{@link fr.inria.corese.core.next.storage.api.operations.MetadataOperations}: Structural summaries
 *       and cardinality statistics used by the cost-based query optimizer (CBO).</li>
 * </ul>
 *
 * <h2>Stream and Resource Lifecycle</h2>
 * <p>Streams returned by {@link fr.inria.corese.core.next.storage.api.operations.QueryOperations#find}
 * are lazy, single-use, and may hold low-level snapshot references. Callers must always close them,
 * preferably using {@code try-with-resources}, to prevent snapshot and partition leaks.</p>
 *
 * <h2>Graph Matching Semantics</h2>
 * <p>Operations accepting {@link fr.inria.corese.core.next.storage.api.model.StatementPattern} follow standard
 * SPARQL dataset semantics:</p>
 * <ul>
 *   <li>A {@code null} or empty contexts array matches across all graphs (the union of default and named graphs).</li>
 *   <li>An explicit {@code null} array element (e.g., {@code new Resource[] { null }}) matches strictly the default graph.</li>
 *   <li>Named graph identifiers restrict matching to the specified named graph(s).</li>
 * </ul>
 */
package fr.inria.corese.core.next.storage.api.operations;
