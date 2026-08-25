package fr.inria.corese.core.next.query.impl.sparql.io.serializer.json;

import fr.inria.corese.core.next.data.api.support.io.AbstractIOOptions;
import fr.inria.corese.core.next.query.api.io.serializer.LinksOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Options to configure the SPARQL results JSON serializer
 */
public class JsonResultSerializerOptions extends AbstractIOOptions implements LinksOptions {
    private final Collection<String> links;

    protected JsonResultSerializerOptions(JsonResultSerializerOptions.Builder builder) {
        this.links = List.copyOf(builder.links);
    }

    @Override
    public Collection<String> links() {
        return links;
    }

    public static class Builder extends AbstractIOOptions.Builder<JsonResultSerializerOptions> {
        private final Collection<String> links;

        public Builder() {
            this.links = new ArrayList<>();
        }

        @Override
        public JsonResultSerializerOptions build() {
            return new JsonResultSerializerOptions(this);
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

    }
}
