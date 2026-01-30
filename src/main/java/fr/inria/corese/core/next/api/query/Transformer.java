package fr.inria.corese.core.next.api.query;

public interface Transformer {

    /**
     *
     * @param queryString String represnetation of a SPARQL query following W3C standards
     * @param queryLanguage Query language. @
     * @return The compiled query object
     */
    Query compile(String queryString, QueryLanguage queryLanguage);

    Query compileSPARQL(String queryString);

}
