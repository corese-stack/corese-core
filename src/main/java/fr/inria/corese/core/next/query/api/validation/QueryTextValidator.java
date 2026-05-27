package fr.inria.corese.core.next.query.api.validation;

import java.io.InputStream;
import java.io.Reader;

/**
 * Public validation entry point for query text.
 *
 * <p>Current SPARQL semantic coverage is intentionally limited to the
 * features already represented by the {@code next} AST and semantic rules.
 * Today this mainly covers syntax diagnostics plus implemented scope checks
 * such as SELECT projection visibility and ORDER BY visibility for
 * SELECT/CONSTRUCT/DESCRIBE.</p>
 *
 * <p>Validation is intended to behave like a linter: malformed or semantically
 * invalid operations should be reported through {@link QueryDiagnostic}
 * instances collected in the returned {@link QueryValidationResult}, rather
 * than through query-invalidity exceptions. Technical failures unrelated to the
 * query text itself, such as I/O failures while reading the input, may still
 * surface as exceptions.</p>
 */
public interface QueryTextValidator extends QueryValidator<String> {

    QueryValidationResult validate(InputStream in);

    QueryValidationResult validate(InputStream in, String baseIRI);

    QueryValidationResult validate(Reader reader);

    QueryValidationResult validate(Reader reader, String baseIRI);

    @Override
    QueryValidationResult validate(String queryString);

    QueryValidationResult validate(String queryString, String baseIRI);
}
