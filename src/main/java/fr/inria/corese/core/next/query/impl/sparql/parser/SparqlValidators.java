package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.validation.QueryTextValidator;

/**
 * Internal entry points for validators backed by the SPARQL parser.
 */
public final class SparqlValidators {

    private SparqlValidators() {
    }

    /** Returns a validator for SPARQL query text. */
    public static QueryTextValidator sparql() {
        return new SparqlParser();
    }
}
