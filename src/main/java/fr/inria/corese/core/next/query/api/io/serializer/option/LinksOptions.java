package fr.inria.corese.core.next.query.api.io.serializer.option;

import java.util.Collection;

/**
 * Serialization options that carry a collection of named links to be embedded
 * in the result output (e.g. the SPARQL JSON results {@code "link"} member).
 */
public interface LinksOptions extends ResultSerializationOptions {

    /**
     * Returns the link names to include in the serialized output.
     *
     * @return a non-null, possibly empty collection of link names
     */
    Collection<String> links();
}
