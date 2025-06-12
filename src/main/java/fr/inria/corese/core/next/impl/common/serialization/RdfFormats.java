package fr.inria.corese.core.next.impl.common.serialization;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Provides a central registry of known RDF serialization formats,
 * along with utility methods for lookup by extension or MIME type.
 */
public final class RdfFormats {

    public static final RdfFormat TURTLE = new RdfFormat(
            "Turtle",
            List.of("ttl"),
            List.of("text/turtle"),
            true,
            true);

    public static final RdfFormat NTRIPLES = new RdfFormat(
            "N-Triples",
            List.of("nt"),
            List.of("application/n-triples", "text/plain"),
            false,
            false);

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
            true);

    public static final RdfFormat NQUADS = new RdfFormat(
            "N-Quads",
            List.of("nq"),
            List.of("application/n-quads"),
            false,
            true);

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
     * Returns all known RDF formats.
     */
    public static List<RdfFormat> all() {
        return List.of(TURTLE, NTRIPLES, JSONLD, RDFXML, NQUADS);
    }

}
