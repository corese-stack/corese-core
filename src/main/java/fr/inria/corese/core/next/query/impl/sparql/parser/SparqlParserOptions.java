package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.data.api.support.io.IOConstants;
import fr.inria.corese.core.next.data.api.io.option.BaseIRIOptions;
import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.sparql.parser.options.ErrorHandlingOptions;
import fr.inria.corese.core.next.query.impl.sparql.parser.options.StrictModeOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration options for the SPARQL parser.
 *
 * <p>This class controls parsing behavior such as base IRI resolution,
 * strict mode validation, and error handling strategy.</p>
 *
 * <h3>Default values</h3>
 * <ul>
 *   <li><b>baseIRI</b>: {@link IOConstants#getDefaultBaseURI()}</li>
 *   <li><b>strictMode</b>: {@code false}</li>
 *   <li><b>failFast</b>: {@code true}</li>
 *   <li><b>collectErrors</b>: {@code true}</li>
 * </ul>
 *
 * <p>Instances are immutable and must be created using the {@link Builder}.</p>
 */
public final class SparqlParserOptions
        implements BaseIRIOptions, StrictModeOptions, ErrorHandlingOptions {

    private final String baseIRI;
    private final boolean strictMode;
    private final boolean failFast;
    private final boolean collectErrors;
    private final List<QueryDiagnostic> diagnostics;


    private SparqlParserOptions(SparqlParserOptions.Builder builder) {
        this.baseIRI = builder.baseIRI;
        this.strictMode = builder.strictMode;
        this.failFast = builder.failFast;
        this.collectErrors = builder.collectErrors;
        this.diagnostics = collectErrors ? new ArrayList<>() : Collections.emptyList();
    }

    /**
     * Returns the base IRI used for resolving relative IRIs.
     *
     * <p><b>Default:</b> {@link IOConstants#getDefaultBaseURI()}</p>
     *
     * @return the configured base IRI (never null)
     */
    @Override
    public String getBaseIRI() {
        return this.baseIRI;
    }

    /**
     * Indicates whether strict parsing mode is enabled.
     *
     * <p>In strict mode, additional validation rules may be enforced
     * (e.g., stricter SPARQL grammar or semantic checks).</p>
     *
     * <p><b>Default:</b> {@code false}</p>
     *
     * @return true if strict mode is enabled
     */
    @Override
    public boolean isStrictMode() {
        return strictMode;
    }

    /**
     * Indicates whether the parser should stop immediately
     * on the first syntax error.
     *
     * <p>If enabled, parsing aborts as soon as an error is encountered.
     * If disabled, the parser attempts error recovery.</p>
     *
     * <p><b>Default:</b> {@code true}</p>
     *
     * @return true if parsing stops at first error
     */
    @Override
    public boolean isFailFast() {
        return failFast;
    }

    /**
     * Indicates whether parsing errors should be collected internally.
     *
     * <p>If enabled, errors are stored and accessible via {@link #getErrors()}.</p>
     *
     * <p><b>Default:</b> {@code true}</p>
     *
     * @return true if errors are collected
     */
    @Override
    public boolean isCollectErrors() {
        return collectErrors;
    }

    /**
     * Returns the collected parsing errors.
     *
     * <p>If {@code collectErrors} is false, this list will always be empty.</p>
     *
     * @return an unmodifiable list of parsing error messages
     */
    @Override
    public List<String> getErrors() {
        if (diagnostics.isEmpty()) return List.of();
        return diagnostics.stream().map(QueryDiagnostic::format).toList();
    }

    @Override
    public List<QueryDiagnostic> getDiagnostics() {
        return Collections.unmodifiableList(diagnostics);
    }

    /**
     * Records a parsing error internally if {@code collectErrors} is enabled.
     *
     * @param diagnostic error message (ignored if null or collection disabled)
     */
    public void addDiagnostic(QueryDiagnostic diagnostic) {
        if (collectErrors && diagnostic != null) {
            diagnostics.add(diagnostic);
        }
    }

    /**
     * Builder for {@link SparqlParserOptions}.
     *
     * <h3>Default values</h3>
     * <ul>
     *   <li>baseIRI = {@link IOConstants#getDefaultBaseURI()}</li>
     *   <li>strictMode = false</li>
     *   <li>failFast = true</li>
     *   <li>collectErrors = true</li>
     * </ul>
     */
    public static final class Builder {

        private String baseIRI = IOConstants.getDefaultBaseURI();

        private boolean strictMode;

        private boolean failFast = true;

        private boolean collectErrors = true;

        public SparqlParserOptions build() {
            return new SparqlParserOptions(this);
        }

        /**
         * Set the base IRI used for relative IRI processing
         * @param baseIRI An IRI
         * @return this
         */
        public SparqlParserOptions.Builder baseIRI(String baseIRI) {
            this.baseIRI = baseIRI;
            return this;
        }

        /**
         * Enables or disables strict parsing mode.
         *
         * @param strictMode true to enable strict mode
         * @return this builder
         */
        public Builder strictMode(boolean strictMode) {
            this.strictMode = strictMode;
            return this;
        }

        /**
         * Enables or disables fail-fast behavior.
         *
         * @param failFast true to stop at first error
         * @return this builder
         */
        public Builder failFast(boolean failFast) {
            this.failFast = failFast;
            return this;
        }

        /**
         * Enables or disables error collection.
         *
         * @param collectErrors true to collect parsing errors
         * @return this builder
         */
        public Builder collectErrors(boolean collectErrors) {
            this.collectErrors = collectErrors;
            return this;
        }

    }

}
