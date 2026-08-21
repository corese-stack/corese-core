/**
 * Concrete implementations of the public repository API.
 *
 * <p>Entry point for users: create a {@link fr.inria.corese.core.next.query.impl.repository.CoreseRepository}
 * backed by any {@link fr.inria.corese.core.next.storage.api.StorageManager},
 * call {@code init()}, then open a
 * {@link fr.inria.corese.core.next.query.api.repository.RepositoryConnection} via
 * {@code getConnection()}.</p>
 *
 * <p>The parser, AST, bridge, and KGRAM engine are invisible from this package.
 * {@link fr.inria.corese.core.next.query.impl.repository.CoreseRepositoryConnection}
 * hides {@link fr.inria.corese.core.next.query.impl.sparql.execution.NextSparqlPipelineExecutor}
 * and validates query syntax at preparation time before returning typed query objects.</p>
 */
package fr.inria.corese.core.next.query.impl.repository;
