package fr.inria.corese.core.next.query.api.sparql.options;

import java.util.List;

public interface ErrorHandlingOptions {

    /**
     * if true stop the parsing and throw an exception
     * @return {@code true} if parsing should fail immediately on first error,
     *         {@code false} otherwise
     */
    boolean isFailFast();

    /**
     * if true collect the error in a List
     * @return {@code true} if errors should be collected,
     *         {@code false} otherwise
     */
    boolean isCollectErrors();

    /**
     * Returns collected errors as human-friendly strings.
     * <p>Prefer {@link #getDiagnostics()} for structured access.</p>
     * @return an unmodifiable list of human-readable error messages;
     *         returns an empty list if error collection is disabled
     *         or no errors were recorded
     */
    List<String> getErrors();
    /**
     * Returns collected errors as structured diagnostics.
     * <p>If {@link #isCollectErrors()} is false, this list is empty.</p>
     * @return an unmodifiable list of structured parsing diagnostics ({@link SparqlAstError});
     *         never {@code null}
     */
    List<SparqlAstError> getDiagnostics();


}
