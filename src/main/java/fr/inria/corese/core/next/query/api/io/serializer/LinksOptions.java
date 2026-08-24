package fr.inria.corese.core.next.query.api.io.serializer;

import fr.inria.corese.core.next.data.api.io.option.IOOptions;

import java.util.Collection;

/**
 * Serialization options that carry a collection of named links to be embedded
 * in the result output (e.g. SPARQL JSON results {@code "links"} member).
 */
public interface LinksOptions extends IOOptions {

    /**
     * Returns the link names to include in the serialized output.
     *
     * @return a non-null, possibly empty collection of link names
     */
    Collection<String> links();
}
