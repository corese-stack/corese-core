/**
 * Public SPARQL query API for Corese.
 *
 * <p>Create a ready-to-use repository through
 * {@link fr.inria.corese.core.next.query.Repositories}. The repository convenience
 * methods cover common queries; obtain a
 * {@link fr.inria.corese.core.next.query.api.repository.RepositoryConnection}
 * when prepared operations or progressive results are needed.</p>
 *
 * <ul>
 *   <li>{@link fr.inria.corese.core.next.query.api.TupleQuery} — SPARQL SELECT</li>
 *   <li>{@link fr.inria.corese.core.next.query.api.BooleanQuery} — SPARQL ASK</li>
 *   <li>{@link fr.inria.corese.core.next.query.api.GraphQuery} — SPARQL CONSTRUCT / DESCRIBE</li>
 *   <li>{@link fr.inria.corese.core.next.query.api.Update} — SPARQL 1.1 UPDATE</li>
 * </ul>
 *
 * <p>All types implement {@link fr.inria.corese.core.next.query.api.Operation}, which provides
 * initial bindings ({@code setBinding}), dataset override ({@code setDataset}),
 * and execution-time limit ({@code setMaxExecutionTime}).
 * Query types additionally expose {@code setTimeout(long millis)} for millisecond precision.</p>
 *
 * <p>No internal types (parser, AST, bridge, KGRAM) appear in this package or its sub-packages.
 * All errors are reported via the exception hierarchy rooted at
 * {@link fr.inria.corese.core.next.query.api.exception.QueryException}.</p>
 */
package fr.inria.corese.core.next.query.api;
