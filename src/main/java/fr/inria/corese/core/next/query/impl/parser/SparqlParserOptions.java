package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.data.impl.io.parser.util.ParserConstants;
import fr.inria.corese.core.next.query.api.sparql.options.AbstractSparqlOptions;
import fr.inria.corese.core.next.query.api.sparql.options.BaseIRIOptions;
import fr.inria.corese.core.next.query.api.sparql.options.ErrorHandlingOptions;
import fr.inria.corese.core.next.query.api.sparql.options.StrictModeOptions;

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
 *   <li><b>baseIRI</b>: {@link ParserConstants#getDefaultBaseURI()}</li>
 *   <li><b>strictMode</b>: {@code false}</li>
 *   <li><b>failFast</b>: {@code true}</li>
 *   <li><b>collectErrors</b>: {@code true}</li>
 * </ul>
 *
 * <p>Instances are immutable and must be created using the {@link Builder}.</p>
 */
public class SparqlParserOptions extends AbstractSparqlOptions
        implements BaseIRIOptions, StrictModeOptions, ErrorHandlingOptions {

    private final SparqlParserOptions.Builder builder;
    private final String baseIRI;
    private final Boolean strictMode;

    private final boolean failFast;
    private final boolean collectErrors;
    private final List<String> errors;


    protected SparqlParserOptions(SparqlParserOptions.Builder builder) {
        this.builder = builder;
        this.baseIRI = this.builder.baseIRI;
        this.strictMode = this.builder.strictMode;
        this.failFast = this.builder.failFast;
        this.collectErrors = this.builder.collectErrors;
        this.errors = builder.collectErrors ? new ArrayList<>() : Collections.emptyList();

    }

    /**
     * Returns the base IRI used for resolving relative IRIs.
     *
     * <p><b>Default:</b> {@link ParserConstants#getDefaultBaseURI()}</p>
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
    public boolean isStrictMode() { return Boolean.TRUE.equals(strictMode); }

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
    public boolean isFailFast() { return failFast; }

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
    public boolean isCollectErrors() { return collectErrors; }

    /**
     * Returns the collected parsing errors.
     *
     * <p>If {@code collectErrors} is false, this list will always be empty.</p>
     *
     * @return an unmodifiable list of parsing error messages
     */
    @Override
    public List<String> getErrors() { return Collections.unmodifiableList(errors); }

    /**
     * Records a parsing error internally if {@code collectErrors} is enabled.
     *
     * @param message error message (ignored if null or collection disabled)
     */
    void addError(String message) {
        if (collectErrors && message != null) errors.add(message);
    }

    /**
     * Builder for {@link SparqlParserOptions}.
     *
     * <h3>Default values</h3>
     * <ul>
     *   <li>baseIRI = {@link ParserConstants#getDefaultBaseURI()}</li>
     *   <li>strictMode = false</li>
     *   <li>failFast = true</li>
     *   <li>collectErrors = true</li>
     * </ul>
     */
    public static class Builder extends AbstractSparqlOptions.Builder<SparqlParserOptions> {

        protected String baseIRI = ParserConstants.getDefaultBaseURI();

        protected Boolean strictMode = false;

        protected Boolean failFast = true;

        protected Boolean collectErrors = true;

        @Override
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
