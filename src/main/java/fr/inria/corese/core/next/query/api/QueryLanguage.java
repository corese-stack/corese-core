package fr.inria.corese.core.next.query.api;

import fr.inria.corese.core.next.query.api.repository.RepositoryConnection;

/**
 * Enumeration of query languages supported by the Corese engine.
 * <p>
 * A {@code QueryLanguage} identifies the syntax and semantics used when
 * preparing queries and updates through
 * {@link RepositoryConnection}.
 * </p>
 *
 *
 * @see RepositoryConnection
 * @see Query
 */
public enum QueryLanguage {

    /**
     * The W3C-standard SPARQL 1.1 query and update language.
     */
    SPARQL
}
