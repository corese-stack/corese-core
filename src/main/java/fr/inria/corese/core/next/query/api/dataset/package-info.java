/**
 * Dataset abstraction and dataset override specifications for SPARQL query evaluation in Corese v5.
 *
 * <h2>SPARQL Dataset Semantics</h2>
 * <p>{@link fr.inria.corese.core.next.query.api.dataset.Dataset} models the {@code FROM} (default graph)
 * and {@code FROM NAMED} (named graph) collections that define the active RDF dataset for query evaluation.</p>
 *
 * <h2>Dataset Override vs. Reset</h2>
 * <ul>
 *   <li><strong>Override:</strong> Providing a non-null {@code Dataset} via
 *       {@link fr.inria.corese.core.next.query.api.repository.RepositoryConnection#setDataset(Dataset)} or
 *       {@link fr.inria.corese.core.next.query.api.Query#setDataset(Dataset)} replaces all {@code FROM} and
 *       {@code FROM NAMED} clauses specified in the SPARQL query text.</li>
 *   <li><strong>Empty Override ({@link fr.inria.corese.core.next.query.api.dataset.Dataset#empty()}):</strong> Explicitly
 *       restricts the dataset to contain zero graphs, yielding empty results.</li>
 *   <li><strong>Reset ({@code null}):</strong> Passing {@code null} removes any active override, restoring the default
 *       behavior where the query text's own clauses or all repository graphs are consulted.</li>
 * </ul>
 *
 * <p>All {@code Dataset} instances are immutable and inherently thread-safe.</p>
 */
package fr.inria.corese.core.next.query.api.dataset;
