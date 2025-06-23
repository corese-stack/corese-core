package fr.inria.corese.core.next.api.base.io;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Describes a semantic RDF serialization format, extending file-level metadata
 * with RDF-specific capabilities.
 * This class also acts as a central registry for all known RDF formats,
 * providing static constants for common formats and utility methods for lookup.
 */
public class RdfFormat extends FileFormat {

    private final boolean supportsNamespaces;
    private final boolean supportsNamedGraphs;

    public static final RdfFormat TURTLE = new RdfFormat(
            "Turtle",
            List.of("ttl"),
            List.of("text/turtle"),
            true,
            false);


    public static final RdfFormat NTRIPLES = new RdfFormat(
            "N-Triples",
            List.of("nt"),
            List.of("application/n-triples", "text/plain"),
            false,
            false);

    public static final RdfFormat NQUADS = new RdfFormat(
            "N-Quads",
            List.of("nq"),
            List.of("application/n-quads"),
            false,
            true);

    public static final RdfFormat JSONLD = new RdfFormat(
            "JSON-LD",
            List.of("jsonld"),
            List.of("application/ld+json", "application/json"),
            true,
            true);

    public static final RdfFormat RDFXML = new RdfFormat(
            "RDF/XML",
            List.of("rdf", "xml"),
            List.of("application/rdf+xml"),
            true,
            false);

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
     * Whether the format supports RDF prefixes (e.g., Turtle's @prefix declarations).
     *
     * @return true if the format supports explicit namespace declarations, false otherwise.
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


    /**
     * Finds a known RDF format by its name (case-insensitive).
     *
     * @param name The name of the format (e.g., "Turtle").
     * @return An Optional containing the matching RdfFormat if found.
     */
    public static Optional<RdfFormat> byName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return all().stream()
                .filter(format -> format.getName().equalsIgnoreCase(n))
                .findFirst();
    }

    /**
     * Finds a known RDF format by file extension (case-insensitive).
     *
     * @param extension The file extension (e.g., "ttl").
     * @return An Optional containing the matching RdfFormat if found.
     */
    public static Optional<RdfFormat> byExtension(String extension) {
        String ext = extension.toLowerCase(Locale.ROOT);
        return all().stream()
                .filter(format -> format.getExtensions().stream()
                        .anyMatch(e -> e.equalsIgnoreCase(ext)))
                .findFirst();
    }

    /**
     * Finds a known RDF format by MIME type (case-insensitive).
     *
     * @param mimeType The MIME type (e.g., "text/turtle").
     * @return An Optional containing the matching RdfFormat if found.
     */
    public static Optional<RdfFormat> byMimeType(String mimeType) {
        String mime = mimeType.toLowerCase(Locale.ROOT);
        return all().stream()
                .filter(format -> format.getMimeTypes().stream()
                        .anyMatch(m -> m.equalsIgnoreCase(mime)))
                .findFirst();
    }

    /**
     * Returns a list of all known RDF formats.
     *
     * @return An unmodifiable List of all RdfFormat constants.
     */
    public static List<RdfFormat> all() {
        return List.of(TURTLE, NTRIPLES, NQUADS, JSONLD, RDFXML);
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