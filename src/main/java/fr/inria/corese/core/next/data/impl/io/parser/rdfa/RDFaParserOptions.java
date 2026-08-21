package fr.inria.corese.core.next.data.impl.io.parser.rdfa;

import fr.inria.corese.core.next.data.api.support.io.AbstractIOOptions;
import fr.inria.corese.core.next.data.api.io.option.BaseIRIOptions;
import fr.inria.corese.core.next.data.api.support.io.IOConstants;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
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
     * @return a Map of URI/boolean features values that will be used to configure the {@link SAXParserFactory}
     */
    public Map<String, Boolean> getSAXFeatures() {
        return this.builder.saxFeatures;
    }

    /**
     * @return a Map of URI/Object properties and handlers used by the {@link javax.xml.parsers.SAXParser}
     */
    public Map<String, Object> getSAXProperties() {
        return this.builder.saxProperties;
    }

    /**
     * @return The schema object used by the XMLReader to validate the document during parsing. null by default.
     */
    public Schema getSchema() {
        return this.builder.schema;
    }

    public static class Builder extends AbstractIOOptions.Builder<RDFaParserOptions> {

        protected String baseIRI = IOConstants.getDefaultBaseURI();
        protected Map<String, Boolean> saxFeatures = new HashMap<>();
        protected Map<String, Object> saxProperties = new HashMap<>();
        private Schema schema = null;

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
         * Sets up the features options for the {@link SAXParserFactory}. See <a href="http://www.saxproject.org/apidoc/org/xml/sax/package-summary.html#package_description">the list of SAX features</a>.
         * @param featureuri the SAX feature URI (e.g. "http://xml.org/sax/features/resolve-dtd-uris")
         * @param value the value desired for the feature
         * @return this
         */
        public RDFaParserOptions.Builder setSAXFeature(String featureuri, boolean value) {
            this.saxFeatures.put(featureuri, value);
            return this;
        }

        /**
         * Sets up the properties options for the {@link javax.xml.parsers.SAXParser}. See <a href="http://www.saxproject.org/apidoc/org/xml/sax/package-summary.html#package_description">the list of SAX handlers and properties</a>.
         * @param propertyUri the SAX property URI (e.g. "http://xml.org/sax/properties/declaration-handler")
         * @param value the value desired for the property
         * @return this
         */
        public RDFaParserOptions.Builder setSAXProperties(String propertyUri, Object value) {
            this.saxProperties.put(propertyUri, value);
            return this;
        }

        /**
         * Sets up the Schema used in the XMLParser for the validation of the document. null by default.
         * @param schema The new {@link Schema}
         * @return this
         */
        public RDFaParserOptions.Builder setSchema(Schema schema) {
            this.schema = schema;
            return this;
        }

    }
}