/**
 * Concrete implementations of the public repository API.
 *
 * <p>This package is internal. Applications create repositories through
 * {@link fr.inria.corese.core.next.query.Repositories} and use the contracts in
 * {@code next.query.api.repository}.</p>
 *
 * <p>The parser, AST, bridge, and KGRAM engine are invisible from this package.
 * {@link fr.inria.corese.core.next.query.impl.repository.CoreseRepositoryConnection}
 * hides {@link fr.inria.corese.core.next.query.impl.sparql.execution.NextSparqlPipelineExecutor}
 * and validates query syntax at preparation time before returning typed query objects.</p>
 */
package fr.inria.corese.core.next.query.impl.repository;
