package fr.inria.corese.core.next.query.api.sparql.options;

import java.util.List;

public interface ErrorHandlingOptions {

    /**
     * if true stop the parsing and throw an exception
     * @return
     */
    boolean isFailFast();

    /**
     * if true collect the error in a List
     * @return
     */
    boolean isCollectErrors();

    /**
     * Returns collected errors as human-friendly strings.
     * <p>Prefer {@link #getDiagnostics()} for structured access.</p>
     * @return
     */
    List<String> getErrors();
    /**
     * Returns collected errors as structured diagnostics.
     * <p>If {@link #isCollectErrors()} is false, this list is empty.</p>
     */
    List<SparqlAstError> getDiagnostics();


}
