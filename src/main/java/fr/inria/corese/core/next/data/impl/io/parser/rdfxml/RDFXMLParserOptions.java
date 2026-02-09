package fr.inria.corese.core.next.data.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.data.api.base.io.AbstractIOOptions;
import fr.inria.corese.core.next.data.api.io.common.BaseIRIOptions;
import fr.inria.corese.core.next.data.impl.io.parser.util.ParserConstants;

/**
 * Configuration class for the parsing of RDF/XML documents
 */
public class RDFXMLParserOptions extends AbstractIOOptions implements BaseIRIOptions {

    private final RDFXMLParserOptions.Builder builder;
    private final String baseIRI;

    protected RDFXMLParserOptions(RDFXMLParserOptions.Builder builder) {
        this.builder = builder;
        this.baseIRI = this.builder.baseIRI;
    }

    @Override
    public String getBaseIRI() {
        return this.baseIRI;
    }

    public static class Builder extends AbstractIOOptions.Builder<RDFXMLParserOptions> {

        protected String baseIRI = ParserConstants.getDefaultBaseURI();

        @Override
        public RDFXMLParserOptions build() {
            return new RDFXMLParserOptions(this);
        }

        /**
         * Set the base IRI used for relative IRI processing
         * @param baseIRI An IRI
         * @return this
         */
        public Builder baseIRI(String baseIRI) {
            this.baseIRI = baseIRI;
            return this;
        }

    }

}
