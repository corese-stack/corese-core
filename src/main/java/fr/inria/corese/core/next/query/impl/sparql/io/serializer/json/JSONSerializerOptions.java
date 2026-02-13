package fr.inria.corese.core.next.query.impl.sparql.io.serializer.json;

import fr.inria.corese.core.next.data.api.base.io.AbstractIOOptions;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Options to configure the SPARQL results JSON serializer
 */
public class JSONSerializerOptions extends AbstractIOOptions {
    private final JSONSerializerOptions.Builder builder;

    protected JSONSerializerOptions(JSONSerializerOptions.Builder builder) {
        this.builder = builder;
    }

    public Collection<String> links() {
        return this.builder.links;
    }

    public static class Builder extends AbstractIOOptions.Builder<JSONSerializerOptions> {
        private final Collection<String> links;

        public Builder() {
            this.links = new ArrayList<>();
        }

        @Override
        public JSONSerializerOptions build() {
            return new JSONSerializerOptions(this);
        }

        /**
         * Adds a link to be added to the header of the SPARQL results.
         * @param link preferably a URI
         * @return this
         */
        public Builder addLink(String link) {
            this.links.add(link);
            return this;
        }

        /**
         * Adds a set of links to be added to the header of the SPARQL results.
         * @param links preferably a set of URIs
         * @return this
         */
        public Builder addLinks(Collection<String> links) {
            this.links.addAll(links);
            return this;
        }
    }
}
