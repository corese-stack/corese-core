package fr.inria.corese.core.next.query.api.validation;

import fr.inria.corese.core.next.query.impl.parser.SparqlParser;

/**
 * Public factories for query validators.
 */
public final class QueryValidators {

    private QueryValidators() {
    }

    /** Returns a validator for SPARQL query text. */
    public static QueryTextValidator sparql() {
        return new SparqlParser();
    }
}
