package fr.inria.corese.core.next.data.impl.io.parser.trig;

import fr.inria.corese.core.next.data.api.support.io.AbstractIOOptions;
import fr.inria.corese.core.next.data.api.io.option.BaseIRIOptions;
import fr.inria.corese.core.next.data.api.io.option.RDFParsingOptions;
import fr.inria.corese.core.next.data.impl.io.parser.support.ParserConstants;

/**
 * Configuration class for the parsing of TriG documents
 */
public class TriGParserOptions extends AbstractIOOptions implements BaseIRIOptions, RDFParsingOptions {

    private final TriGParserOptions.Builder builder;
    private final String baseIRI;

    protected TriGParserOptions(TriGParserOptions.Builder builder) {
        this.builder = builder;
        this.baseIRI = this.builder.baseIRI;
    }

    @Override
    public String getBaseIRI() {
        return this.baseIRI;
    }

    public static class Builder extends AbstractIOOptions.Builder<TriGParserOptions> {

        protected String baseIRI = ParserConstants.getDefaultBaseURI();

        @Override
        public TriGParserOptions build() {
            return new TriGParserOptions(this);
        }

        /**
         * Set the base IRI used for relative IRI processing
         * @param baseIRI An IRI
         * @return this
         */
        public TriGParserOptions.Builder baseIRI(String baseIRI) {
            this.baseIRI = baseIRI;
            return this;
        }

    }

}
