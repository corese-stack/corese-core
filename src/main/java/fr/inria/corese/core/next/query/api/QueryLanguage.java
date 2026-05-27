package fr.inria.corese.core.next.query.api;

import fr.inria.corese.core.next.query.api.repository.RepositoryConnection;

/**
 * Enumeration of query languages supported by the Corese engine.
 * <p>
 * A {@code QueryLanguage} identifies the syntax and semantics used when
 * preparing operations and updates through
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
    SPARQL("SPARQL"),

    /**
     * Corese’s extension language allowing procedural and functional constructs.
     */
    LDSCRIPT("LDScript");

    private final String name;

    QueryLanguage(String name) { this.name = name; }

    /**
     * Returns the canonical name of the query language.
     *
     * @return the language name as a string
     */
    public String getName() { return name; }

    @Override
    public String toString() { return name; }
}