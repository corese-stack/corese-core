/**
 * Closeable result types and solution bindings for SPARQL query evaluation in Corese v5.
 *
 * <h2>Result Types</h2>
 * <ul>
 *   <li>{@link fr.inria.corese.core.next.query.api.result.TupleQueryResult}: Solution sequence for SPARQL SELECT queries,
 *       maintaining deterministic projection variable order via {@code getBindingNames()}.</li>
 *   <li>{@link fr.inria.corese.core.next.query.api.result.GraphQueryResult} &amp;
 *       {@link fr.inria.corese.core.next.query.api.result.StatementResult}: Progressive sequences of RDF statements
 *       produced by SPARQL CONSTRUCT and DESCRIBE queries or repository scans.</li>
 *   <li>{@link fr.inria.corese.core.next.query.api.result.BindingSet} &amp;
 *       {@link fr.inria.corese.core.next.query.api.result.Binding}: Immutable mappings of variable names to RDF terms.</li>
 * </ul>
 *
 * <h2>Stream and Resource Lifecycle</h2>
 * <p>Query results retain active storage snapshot references until exhausted, closed, or interrupted.
 * The {@code stream()} method exposes a standard Java {@link java.util.stream.Stream} registered with an {@code onClose}
 * hook that closes the parent result. Because terminal stream operations (such as {@code findFirst()}, {@code count()},
 * or {@code forEach()}) do <em>not</em> trigger stream closure, callers must manage query results and their streams
 * using {@code try-with-resources}.</p>
 */
package fr.inria.corese.core.next.query.api.result;
