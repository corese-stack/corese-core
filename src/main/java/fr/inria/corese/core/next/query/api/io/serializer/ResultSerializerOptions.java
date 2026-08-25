package fr.inria.corese.core.next.query.api.io.serializer;

import fr.inria.corese.core.next.data.api.io.serializer.option.LineEndingOptions;
import fr.inria.corese.core.next.data.api.support.io.IOConstants;
import fr.inria.corese.core.next.query.api.io.serializer.option.LinksOptions;
import fr.inria.corese.core.next.query.api.io.serializer.option.XmlSerializationOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Public, format-neutral options for SPARQL result serialization.
 *
 * <p>Links apply to JSON and XML. Line endings apply to CSV and TSV. XML
 * output properties apply only to XML; the factory rejects options that a
 * selected format cannot represent instead of silently ignoring them.</p>
 */
public final class ResultSerializerOptions
        implements LinksOptions, LineEndingOptions, XmlSerializationOptions {

    private final List<String> links;
    private final String lineEnding;
    private final Map<String, String> xmlOutputProperties;

    private ResultSerializerOptions(Builder builder) {
        this.links = List.copyOf(builder.links);
        this.lineEnding = builder.lineEnding;
        this.xmlOutputProperties = Map.copyOf(builder.xmlOutputProperties);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ResultSerializerOptions defaults() {
        return builder().build();
    }

    @Override
    public Collection<String> links() {
        return links;
    }

    @Override
    public String getLineEnding() {
        return lineEnding;
    }

    @Override
    public Map<String, String> xmlOutputProperties() {
        return xmlOutputProperties;
    }

    public static final class Builder {
        private final List<String> links = new ArrayList<>();
        private String lineEnding = IOConstants.DEFAULT_LINE_ENDING;
        private final Map<String, String> xmlOutputProperties = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder addLink(String link) {
            links.add(Objects.requireNonNull(link, "link"));
            return this;
        }

        public Builder links(Collection<String> links) {
            Objects.requireNonNull(links, "links").forEach(this::addLink);
            return this;
        }

        public Builder lineEnding(String lineEnding) {
            this.lineEnding = Objects.requireNonNull(lineEnding, "lineEnding");
            return this;
        }

        public Builder xmlOutputProperty(String name, String value) {
            xmlOutputProperties.put(
                    Objects.requireNonNull(name, "name"),
                    Objects.requireNonNull(value, "value"));
            return this;
        }

        public ResultSerializerOptions build() {
            return new ResultSerializerOptions(this);
        }
    }
}
