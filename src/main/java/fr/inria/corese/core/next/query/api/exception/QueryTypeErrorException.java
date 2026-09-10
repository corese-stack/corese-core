package fr.inria.corese.core.next.query.api.exception;

/**
 * A SPARQL expression error (unbound argument, incompatible RDF type or invalid
 * value). FILTER, BIND, projection and the SPARQL error-handling functions may
 * absorb this exception; infrastructure and programming failures must propagate.
 */
@SuppressWarnings("java:S110") // Retains the public query exception hierarchy and distinguishes SPARQL type errors.
public final class QueryTypeErrorException extends QueryEvaluationException {
    public QueryTypeErrorException(String message) {
        super(message);
    }

    public QueryTypeErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
