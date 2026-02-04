package fr.inria.corese.core.next.data.api.query;

import fr.inria.corese.core.next.data.api.query.exception.QuerySyntaxException;

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

    /**
     * Convenience method to compile a SPARQL query.
     *
     * @param queryString the SPARQL query text
     * @return the compiled query
     * @throws QuerySyntaxException if the query string is syntactically invalid
     */
    default Query<?> compileSPARQL(String queryString) throws QuerySyntaxException {
        return compileQuery(queryString, QueryLanguage.SPARQL);
    }

    /**
     * Convenience method to compile a SPARQL UPDATE.
     *
     * @param updateString the SPARQL UPDATE text
     * @return the compiled update
     * @throws QuerySyntaxException if the update string is syntactically invalid
     */
    default Update compileSPARQLUpdate(String updateString) throws QuerySyntaxException {
        return compileUpdate(updateString, QueryLanguage.SPARQL);
    }
}