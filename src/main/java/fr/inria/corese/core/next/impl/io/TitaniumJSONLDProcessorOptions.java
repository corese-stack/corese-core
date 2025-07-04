package fr.inria.corese.core.next.impl.io;

import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.JsonLdVersion;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.base.io.AbstractIOOptions;
import fr.inria.corese.core.next.api.io.parser.ParserBaseIRIOptions;

import java.net.URI;
import java.time.Duration;

/**
 * Wrapper around the JsonLdOptions class for the Titanium JSONLD parser and serializer.
 * @see <a href="https://javadoc.io/doc/com.apicatalog/titanium-json-ld/latest/com/apicatalog/jsonld/JsonLdOptions.html">JsonLdOptions</a>
 */
public class TitaniumJSONLDProcessorOptions extends AbstractIOOptions implements ParserBaseIRIOptions {

    private final Builder builder;

    protected TitaniumJSONLDProcessorOptions(Builder builder) {
        this.builder = builder;
    }

    public boolean isCompactArrays() {
        return this.builder.options.isCompactArrays();
    }

    public boolean isCompactToRelative() {
        return this.builder.options.isCompactToRelative();
    }

    public boolean isExtractAllScripts() {
        return this.builder.options.isExtractAllScripts();
    }

    public boolean isOmitDefault() {
        return this.builder.options.isOmitDefault();
    }

    public boolean isOmitGraph() {
        return this.builder.options.isOmitGraph();
    }

    public boolean isOrdered() {
        return this.builder.options.isOrdered();
    }

    public JsonLdVersion getProcessingMode() {
        return this.builder.options.getProcessingMode();
    }

    public Duration getTimeout() {
        return this.builder.options.getTimeout();
    }

    public boolean isUseNativeTypes() {
        return this.builder.options.isUseNativeTypes();
    }

    public boolean isUseRdfType() {
        return this.builder.options.isUseRdfType();
    }

    public JsonLdOptions getJsonLdOptions() {
        return this.builder.options;
    }

    @Override
    public String getBase() {
        return this.builder.options.getBase().toString();
    }

    public static class Builder extends AbstractIOOptions.Builder<TitaniumJSONLDProcessorOptions> {

        private final JsonLdOptions options = new JsonLdOptions();

        @Override
        public TitaniumJSONLDProcessorOptions build() {
            return new TitaniumJSONLDProcessorOptions(this);
        }

        public Builder base(String base) {
            this.options.setBase(URI.create(base));
            return this;
        }

        public Builder base(IRI baseIRI) {
            this.options.setBase(URI.create(baseIRI.stringValue()));
            return this;
        }

        public Builder compactArrays(boolean compactArrays) {
            this.options.setCompactArrays(compactArrays);
            return this;
        }

        public Builder compactToRelative(boolean compactToRelative) {
            this.options.setCompactToRelative(compactToRelative);
            return this;
        }

        public Builder extractAllScripts(boolean extractAllScripts) {
            this.options.setExtractAllScripts(extractAllScripts);
            return this;
        }

        public Builder omitDefault(boolean omitDefault) {
            this.options.setOmitDefault(omitDefault);
            return this;
        }

        public Builder omitGraph(boolean omitGraph) {
            this.options.setOmitGraph(omitGraph);
            return this;
        }

        public Builder ordered(boolean ordered) {
            this.options.setOrdered(ordered);
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.options.setTimeout(timeout);
            return this;
        }

        public Builder useRdfType(boolean useRdfType) {
            this.options.setUseRdfType(useRdfType);
            return this;
        }

        public Builder useNativeTypes(boolean useNativeTypes) {
            this.options.setUseNativeTypes(useNativeTypes);
            return this;
        }

        public Builder processingMode(JsonLdVersion processingMode) {
            this.options.setProcessingMode(processingMode);
            return this;
        }
    }
}
