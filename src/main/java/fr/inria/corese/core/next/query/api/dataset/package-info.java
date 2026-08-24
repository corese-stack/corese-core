/**
 * Dataset abstraction for SPARQL query evaluation scope.
 *
 * <p>{@link fr.inria.corese.core.next.query.api.dataset.Dataset} models the FROM / FROM NAMED
 * graph sets that restrict which graphs are visible during query execution.
 * It can be applied at the connection level
 * ({@link fr.inria.corese.core.next.query.api.repository.RepositoryConnection#setDataset})
 * or overridden per individual query
 * ({@link fr.inria.corese.core.next.query.api.Operation#setDataset}).</p>
 */
package fr.inria.corese.core.next.query.api.dataset;
