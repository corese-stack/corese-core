package fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml;

import fr.inria.corese.core.next.data.api.base.io.AbstractIOOptions;
import fr.inria.corese.core.next.query.api.io.serializer.LinksOptions;

import javax.xml.transform.OutputKeys;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class XMLSerializerOptions  extends AbstractIOOptions implements LinksOptions {
    private final XMLSerializerOptions.Builder builder;

    protected XMLSerializerOptions(XMLSerializerOptions.Builder builder) {
        this.builder = builder;
    }

    public final Map<String, String> getXmlSettings() {
        return this.builder.xmlSettings;
    }

    @Override
    public Collection<String> links() {
        return this.builder.links;
    }

    public static class Builder extends AbstractIOOptions.Builder<XMLSerializerOptions> {
        private final Map<String, String> xmlSettings;
        private final Collection<String> links;

        public Builder() {
            this.xmlSettings = new HashMap<>();
            this.xmlSettings.put(OutputKeys.STANDALONE, XMLSerializerConstants.YES_PROPERTY_VALUE);
            this.links = new ArrayList<>();
        }

        /**
         * Set the value for a setting recognized by the XML serializer. the key values are available as constants in {@link javax.xml.transform.OutputKeys }
         * @param key the url of the desired feature
         * @param value the value for the feature
         * @return this
         */
        public Builder setXMLSetting(String key, String value) {
            this.xmlSettings.put(key, value);
            return this;
        }

        /**
         * Adds a link to be added to the header of the SPARQL results.
         * @param link preferably a URI
         * @return this
         */
        public XMLSerializerOptions.Builder addLink(String link) {
            this.links.add(link);
            return this;
        }

        /**
         * Adds a set of links to be added to the header of the SPARQL results.
         * @param links preferably a set of URIs
         * @return this
         */
        public XMLSerializerOptions.Builder addLinks(Collection<String> links) {
            this.links.addAll(links);
            return this;
        }

        @Override
        public XMLSerializerOptions build() {
            return new XMLSerializerOptions(this);
        }
    }
}
