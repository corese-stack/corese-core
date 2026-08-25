package fr.inria.corese.core.next.query.impl.sparql.io.serializer.xml;

import fr.inria.corese.core.next.data.api.support.io.AbstractIOOptions;
import fr.inria.corese.core.next.query.api.io.serializer.option.LinksOptions;
import fr.inria.corese.core.next.query.api.io.serializer.option.XmlSerializationOptions;

import javax.xml.transform.OutputKeys;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlResultSerializerOptions extends AbstractIOOptions implements LinksOptions, XmlSerializationOptions {
    private final Map<String, String> xmlSettings;
    private final Collection<String> links;

    protected XmlResultSerializerOptions(XmlResultSerializerOptions.Builder builder) {
        this.xmlSettings = Map.copyOf(builder.xmlSettings);
        this.links = List.copyOf(builder.links);
    }

    public final Map<String, String> getXmlSettings() {
        return xmlSettings;
    }

    @Override
    public Map<String, String> xmlOutputProperties() {
        return xmlSettings;
    }

    @Override
    public Collection<String> links() {
        return links;
    }

    public static class Builder extends AbstractIOOptions.Builder<XmlResultSerializerOptions> {
        private final Map<String, String> xmlSettings;
        private final Collection<String> links;

        public Builder() {
            this.xmlSettings = new HashMap<>();
            this.xmlSettings.put(OutputKeys.STANDALONE, XmlResultConstants.YES_PROPERTY_VALUE);
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

        @Override
        public XmlResultSerializerOptions build() {
            return new XmlResultSerializerOptions(this);
        }
    }
}
