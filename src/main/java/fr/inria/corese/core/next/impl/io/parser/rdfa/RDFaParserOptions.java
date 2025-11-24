package fr.inria.corese.core.next.impl.io.parser.rdfa;

import fr.inria.corese.core.next.api.base.io.AbstractIOOptions;
import fr.inria.corese.core.next.api.io.common.BaseIRIOptions;
import fr.inria.corese.core.next.impl.io.common.IOConstants;
import fr.inria.corese.core.next.impl.io.parser.util.ParserConstants;

/**
 * Configuration class for the parsing of RDFa HTML documents
 */
public class RDFaParserOptions extends AbstractIOOptions implements BaseIRIOptions {

    private final RDFaParserOptions.Builder builder;
    private final String baseIRI;

    protected RDFaParserOptions(RDFaParserOptions.Builder builder) {
        this.builder = builder;
        this.baseIRI = this.builder.baseIRI;
    }

    @Override
    public String getBaseIRI() {
        return this.baseIRI;
    }

    public static class Builder extends AbstractIOOptions.Builder<RDFaParserOptions> {

        protected String baseIRI = IOConstants.getDefaultBaseURI();

        @Override
        public RDFaParserOptions build() {
            return new RDFaParserOptions(this);
        }

        /**
         * Set the base IRI used for relative IRI processing
         *
         * @param baseIRI An IRI
         * @return this
         */
        public RDFaParserOptions.Builder baseIRI(String baseIRI) {
            this.baseIRI = baseIRI;
            return this;
        }

    }
}