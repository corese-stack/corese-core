package fr.inria.corese.core.next.impl.common.serialization;

import java.util.List;
import java.util.Objects;

/**
 * Describes a semantic RDF serialization format, extending file-level metadata
 * with RDF-specific capabilities.
 */
public class RdfFormat extends FileFormat {

    private final boolean supportsNamespaces;
    private final boolean supportsNamedGraphs;

    /**
     * Constructs a new RDF format.
     *
     * @param name                The name of the format (e.g., "Turtle").
     * @param extensions          File extensions for this format (e.g., ["ttl"]).
     * @param mimeTypes           MIME types for this format (e.g.,
     *                            ["text/turtle"]).
     * @param supportsNamespaces  Whether the format supports prefixes/namespaces.
     * @param supportsNamedGraphs Whether the format supports named graphs.
     *                            serialization.
     */
    public RdfFormat(
            String name,
            List<String> extensions,
            List<String> mimeTypes,
            boolean supportsNamespaces,
            boolean supportsNamedGraphs) {
        super(name, extensions, mimeTypes);
        this.supportsNamespaces = supportsNamespaces;
        this.supportsNamedGraphs = supportsNamedGraphs;
    }

    /**
     * Whether the format supports RDF prefixes (e.g., Turtle).
     */
    public boolean supportsNamespaces() {
        return supportsNamespaces;
    }

    /**
     * Whether the format supports named graphs (e.g., TriG, N-Quads).
     */
    public boolean supportsNamedGraphs() {
        return supportsNamedGraphs;
    }

    @Override
    public String toString() {
        return "%s [extensions: %s, mimeTypes: %s, prefixes: %s, namedGraphs: %s]".formatted(
                getName(),
                String.join(", ", getExtensions()),
                String.join(", ", getMimeTypes()),
                supportsNamespaces(),
                supportsNamedGraphs());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof RdfFormat other))
            return false;
        return getName().equalsIgnoreCase(other.getName())
                && getExtensions().equals(other.getExtensions())
                && getMimeTypes().equals(other.getMimeTypes())
                && supportsNamespaces == other.supportsNamespaces
                && supportsNamedGraphs == other.supportsNamedGraphs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getName().toLowerCase(),
                getExtensions(),
                getMimeTypes(),
                supportsNamespaces,
                supportsNamedGraphs);
    }    

}
