package fr.inria.corese.core.next.query.api.validation;

/**
 * Validates a query model and returns structured diagnostics.
 *
 * @param <T> validated input type
 */
public interface QueryValidator<T> {

    /**
     * Validates an input and returns all collected diagnostics.
     *
     * <p>Implementations should prefer returning diagnostics over throwing when
     * the input is malformed or semantically invalid. Runtime failures unrelated
     * to validation may still surface as exceptions.</p>
     */
    QueryValidationResult validate(T input);
}
