package fr.inria.corese.core.next.query.sparql.parser;

import fr.inria.corese.core.next.data.impl.io.parser.util.ParserConstants;
import fr.inria.corese.core.next.query.sparql.options.AbstractSparqlOptions;
import fr.inria.corese.core.next.query.sparql.options.BaseIRIOptions;
import fr.inria.corese.core.next.query.sparql.options.ErrorHandlingOptions;
import fr.inria.corese.core.next.query.sparql.options.StrictModeOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @Override
    public String getBaseIRI() {
        return this.baseIRI;
    }

    @Override
    public boolean isStrictMode() { return Boolean.TRUE.equals(strictMode); }

    @Override
    public boolean isFailFast() { return failFast; }

    @Override
    public boolean isCollectErrors() { return collectErrors; }

    @Override
    public List<String> getErrors() { return Collections.unmodifiableList(errors); }

    /** parser can record errors if collectErrors=true */
    void addError(String message) {
        if (collectErrors && message != null) errors.add(message);
    }

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

        public Builder strictMode(boolean strictMode) {
            this.strictMode = strictMode;
            return this;
        }

        public Builder failFast(boolean failFast) {
            this.failFast = failFast;
            return this;
        }

        public Builder collectErrors(boolean collectErrors) {
            this.collectErrors = collectErrors;
            return this;
        }

    }

}
