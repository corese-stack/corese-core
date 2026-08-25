package fr.inria.corese.core.next.query.api.io.serializer;

import java.util.Collection;

/**
 * Serialization options that carry a collection of named links to be embedded
 * in the result output (e.g. SPARQL JSON results {@code "links"} member).
 */
public interface LinksOptions extends ResultIOOptions {

    /**
     * Returns the link names to include in the serialized output.
     *
     * @return a non-null, possibly empty collection of link names
     */
    Collection<String> links();
}
