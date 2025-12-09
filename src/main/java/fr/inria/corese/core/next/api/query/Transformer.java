package fr.inria.corese.core.next.api.query;

public interface Transformer {

    Query compile(String queryString, QueryLanguage queryLanguage);

}
