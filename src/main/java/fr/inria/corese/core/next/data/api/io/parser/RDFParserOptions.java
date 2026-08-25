package fr.inria.corese.core.next.data.api.io.parser;

import fr.inria.corese.core.next.data.api.io.option.BaseIRIOptions;
import fr.inria.corese.core.next.data.api.io.option.RDFParsingOptions;

/** Public options shared by RDF parsers. */
public final class RDFParserOptions implements RDFParsingOptions, BaseIRIOptions {

    private final String baseIRI;

    private RDFParserOptions(Builder builder) {
        this.baseIRI = builder.baseIRI;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getBaseIRI() {
        return baseIRI;
    }

    public static final class Builder {
        private String baseIRI;

        private Builder() {
        }

        public Builder baseIRI(String baseIRI) {
            this.baseIRI = baseIRI;
            return this;
        }

        public RDFParserOptions build() {
            return new RDFParserOptions(this);
        }
    }
}
