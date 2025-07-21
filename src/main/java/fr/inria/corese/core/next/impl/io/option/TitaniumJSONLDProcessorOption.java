package fr.inria.corese.core.next.impl.io.option;

import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.JsonLdVersion;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.base.io.AbstractIOOptions;
import fr.inria.corese.core.next.api.io.parser.RDFParserBaseIRIOptions;
import fr.inria.corese.core.next.api.io.parser.RDFParserOptions;

import java.net.URI;
import java.time.Duration;

/**
 * Wrapper around the JsonLdOptions class for the Titanium JSONLD parser and serializer.
 * @see <a href="https://javadoc.io/doc/com.apicatalog/titanium-json-ld/latest/com/apicatalog/jsonld/JsonLdOptions.html">JsonLdOptions</a>
 */
public class TitaniumJSONLDProcessorOption extends AbstractIOOptions implements RDFParserOptions, RDFParserBaseIRIOptions {

    private final Builder builder;

    protected TitaniumJSONLDProcessorOption(Builder builder) {
        this.builder = builder;
    }

    /**
     * @return it true, the serializer will compact arrays of length 1 with a single object item will be compacted to use only that object instead. <a href="https://www.w3.org/TR/json-ld11-api/#dom-jsonldoptions-compactarrays">See standard.</a>
     */
    public boolean compactsArrays() {
        return this.builder.options.isCompactArrays();
    }

    /**
     * @return if true, the serializer will compact IRIs to be relative to document base. <a href="https://www.w3.org/TR/json-ld11-api/#dom-jsonldoptions-compacttorelative">See standard.</a>
     */
    public boolean compactsToRelative() {
        return this.builder.options.isCompactToRelative();
    }

    /**
     * @return If  true, when extracting JSON-LD script elements from HTML, unless a specific fragment identifier is targeted, extracts all encountered JSON-LD script elements using an array form, if necessary. <a href="https://www.w3.org/TR/json-ld11-api/#dom-jsonldoptions-extractallscripts">See standard.</a>
     */
    public boolean isExtractAllScripts() {
        return this.builder.options.isExtractAllScripts();
    }

    public boolean omitsDefault() {
        return this.builder.options.isOmitDefault();
    }

    public boolean omitGraphs() {
        return this.builder.options.isOmitGraph();
    }

    /**
     *
     * @return If true, the serializer will produce ordered JSON-LD. <a href="https://www.w3.org/TR/json-ld11-api/#dom-jsonldoptions-ordered">See standard.</a>
     */
    public boolean isOrdered() {
        return this.builder.options.isOrdered();
    }

    /**
     *
     * @return the version of the JSON-LD standard used for processing. <a href="https://www.w3.org/TR/json-ld11-api/#dom-jsonldoptions-processingmode">See standard.</a>
     */
    public JsonLdVersion getProcessingMode() {
        return this.builder.options.getProcessingMode();
    }

    /**
     * @return the processing timeout
     */
    public Duration getTimeout() {
        return this.builder.options.getTimeout();
    }

    /**
     * @return If true, the serializer will produce JSON-LD using native types. <a href="https://www.w3.org/TR/json-ld11-api/#dom-jsonldoptions-usenativetypes">See standard.</a>
     */
    public boolean usesNativeTypes() {
        return this.builder.options.isUseNativeTypes();
    }

    /**
     * @return If true, the serializer will produce JSON-LD using RDF type. <a href="https://www.w3.org/TR/json-ld11-api/#dom-jsonldoptions-userdftype">See standard.</a>
     */
    public boolean usesRdfType() {
        return this.builder.options.isUseRdfType();
    }

    /**
     *
     * @return the inner JsonLdOptions object.
     */
    public JsonLdOptions getJsonLdOptions() {
        return this.builder.options;
    }

    @Override
    public String getBase() {
        return this.builder.options.getBase().toString();
    }

    public static class Builder extends AbstractIOOptions.Builder<TitaniumJSONLDProcessorOption> {

        private final JsonLdOptions options = new JsonLdOptions();

        @Override
        public TitaniumJSONLDProcessorOption build() {
            return new TitaniumJSONLDProcessorOption(this);
        }

        /**
         * @param base the base IRI to use for resolving relative IRIs.
         * @return this builder
         */
        public Builder base(String base) {
            this.options.setBase(URI.create(base));
            return this;
        }

        /**
         * @param baseIRI the base IRI to use for resolving relative IRIs.
         * @return this builder
         */
        public Builder base(IRI baseIRI) {
            this.options.setBase(URI.create(baseIRI.stringValue()));
            return this;
        }

        /**
         *
         * @param compactArrays if true, arrays of length 1 with a single object item will be compacted to use only that object instead.
         * @return this builder
         */
        public Builder compactArrays(boolean compactArrays) {
            this.options.setCompactArrays(compactArrays);
            return this;
        }

        /**
         * @param compactToRelative if true, compact IRIs to be relative to document base.
         * @return this builder
         */
        public Builder compactToRelative(boolean compactToRelative) {
            this.options.setCompactToRelative(compactToRelative);
            return this;
        }

        /**
         * @param extractAllScripts If set to true, when extracting JSON-LD script elements from HTML, unless a specific fragment identifier is targeted, extracts all encountered JSON-LD script elements using an array form, if necessary.
         * @return this builder
         */
        public Builder extractAllScripts(boolean extractAllScripts) {
            this.options.setExtractAllScripts(extractAllScripts);
            return this;
        }

        public Builder omitGraph(boolean omitGraph) {
            this.options.setOmitGraph(omitGraph);
            return this;
        }

        public Builder omitDefault(boolean omitDefault) {
            this.options.setOmitDefault(omitDefault);
            return this;
        }

        /**
         *
         * @param ordered If set to true, certain algorithm processing steps where indicated are ordered lexicographically.
         * @return this builder
         */
        public Builder ordered(boolean ordered) {
            this.options.setOrdered(ordered);
            return this;
        }

        /**
         * @param timeout the processing timeout.
         * @return this builder
         */
        public Builder timeout(Duration timeout) {
            this.options.setTimeout(timeout);
            return this;
        }

        /**
         * @param useRdfType if true, use rdf:type instead of @type in serialization.
         * @return this builder
         */
        public Builder useRdfType(boolean useRdfType) {
            this.options.setUseRdfType(useRdfType);
            return this;
        }

        /**
         * @param useNativeTypes Causes the Serialize RDF as JSON-LD Algorithm to use native JSON values in value objects avoiding the need for an explicitly @type.
         * @return this builder
         */
        public Builder useNativeTypes(boolean useNativeTypes) {
            this.options.setUseNativeTypes(useNativeTypes);
            return this;
        }

        /**
         *
         * @param processingMode the version of the JSON-LD specification to use.
         * @return this builder
         */
        public Builder processingMode(JsonLdVersion processingMode) {
            this.options.setProcessingMode(processingMode);
            return this;
        }
    }
}
