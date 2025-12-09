package fr.inria.corese.core.next.api.query;

/**
 * Enumeration of query languages supported by the Corese engine.
 * <p>
 * A {@code QueryLanguage} identifies the syntax and semantics used when
 * preparing queries and updates through
 * {@link fr.inria.corese.core.next.api.repository.RepositoryConnection}.
 * </p>
 *
 *
 * @see fr.inria.corese.core.next.api.repository.RepositoryConnection
 * @see fr.inria.corese.core.next.api.query.Query
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