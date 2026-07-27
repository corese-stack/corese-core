package fr.inria.corese.core.next.query.api;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;

/**
 * Compiles SPARQL query and update strings into executable query objects.
 */
public interface Transformer {

    /**
     * Compiles a SPARQL query string into an executable query object.
     *
     * @param queryString the SPARQL query text (SELECT, CONSTRUCT, DESCRIBE, or ASK)
     * @param queryLanguage the query language (typically {@link QueryLanguage#SPARQL})
     * @return the compiled query (TupleQuery, GraphQuery, or BooleanQuery)
     * @throws QuerySyntaxException if the query string is syntactically invalid
     */
    Query<?> compileQuery(String queryString, QueryLanguage queryLanguage)
            throws QuerySyntaxException;

    /**
     * Compiles a SPARQL UPDATE string into an executable update object.
     *
     * @param updateString the SPARQL UPDATE text
     * @param queryLanguage the update language (typically {@link QueryLanguage#SPARQL})
     * @return the compiled update operation
     * @throws QuerySyntaxException if the update string is syntactically invalid
     */
    Update compileUpdate(String updateString, QueryLanguage queryLanguage)
            throws QuerySyntaxException;

}