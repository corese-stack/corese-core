package fr.inria.corese.core.next.data.impl.io.parser.rdfa;

import fr.inria.corese.core.next.data.api.base.io.AbstractIOOptions;
import fr.inria.corese.core.next.data.io.common.BaseIRIOptions;
import fr.inria.corese.core.next.data.impl.io.common.IOConstants;

import javax.xml.parsers.SAXParserFactory;
import java.util.HashMap;
import java.util.Map;

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

    /**
     *
     * @return a Map of URI/boolean features values that will be used to configure the SAXParser
     */
    public Map<String, Boolean> getSAXFeatures() {
        return this.builder.saxFeatures;
    }

    public static class Builder extends AbstractIOOptions.Builder<RDFaParserOptions> {

        protected String baseIRI = IOConstants.getDefaultBaseURI();
        protected Map<String, Boolean> saxFeatures = new HashMap<>();

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

        /**
         * Set up the options for the {@link SAXParserFactory}. See <a href="http://www.saxproject.org/apidoc/org/xml/sax/package-summary.html#package_description">the list of SAX features</a>.
         * @param featureuri the SAX feature URI (e.g. "http://xml.org/sax/features/resolve-dtd-uris")
         * @param value the value desired for the feature
         * @return this
         */
        public RDFaParserOptions.Builder setSAXFeature(String featureuri, boolean value) {
            this.saxFeatures.put(featureuri, value);
            return this;
        }

    }
}