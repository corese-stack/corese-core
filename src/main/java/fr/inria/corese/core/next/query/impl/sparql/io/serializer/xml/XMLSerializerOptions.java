package fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml;

import fr.inria.corese.core.next.data.api.base.io.AbstractIOOptions;

import java.util.HashMap;
import java.util.Map;

public class XMLSerializerOptions  extends AbstractIOOptions {
    private final XMLSerializerOptions.Builder builder;

    protected XMLSerializerOptions(XMLSerializerOptions.Builder builder) {
        this.builder = builder;
    }

    public final Map<String, String> getXmlSettings() {
        return this.builder.xmlSettings;
    }

    public static class Builder extends AbstractIOOptions.Builder<XMLSerializerOptions> {
        private final Map<String, String> xmlSettings;

        public Builder() {
            this.xmlSettings = new HashMap<>();
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

        @Override
        public XMLSerializerOptions build() {
            return new XMLSerializerOptions(this);
        }
    }
}
