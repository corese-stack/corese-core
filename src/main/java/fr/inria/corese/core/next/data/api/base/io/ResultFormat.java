package fr.inria.corese.core.next.api.base.io;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Describes the SPARQL results serialization formats. Defines the static constants representing the standard formats for result representation.
 */
public class ResultFormat extends FileFormat {

    public static final ResultFormat CSV = new ResultFormat(
            "CSV",
            List.of("csv"),
            List.of("text/csv")
    );

    public static final ResultFormat TSV = new ResultFormat(
            "TSV",
            List.of("tsv"),
            List.of("text/tab-separated-values")
    );

    public static final ResultFormat JSON = new ResultFormat(
            "JSON",
            List.of("json"),
            List.of("application/sparql-results+json")
    );

    public static final ResultFormat XML = new ResultFormat(
            "XML",
            List.of("xml", "srx"),
            List.of("application/sparql-results+xml")
    );

    /**
     * Constructs a new ResultFormat instance.
     *
     * @param name       The human-readable name of the format.
     * @param extensions The list of file extensions.
     * @param mimeTypes  The list of MIME types.
     * @throws NullPointerException if name, extensions or mimeTypes is null or
     *                              empty.
     */
    public ResultFormat(
            String name,
            List<String> extensions,
            List<String> mimeTypes) {
        super(name, extensions, mimeTypes);
    }

    /**
     * Finds a known SPARQL result format by its name (case-insensitive).
     *
     * @param name The name of the format (e.g., "XML").
     * @return An Optional containing the matching ResultFormat if found.
     */
    public static Optional<ResultFormat> byName(String name) {
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
    public static Optional<ResultFormat> byExtension(String extension) {
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
    public static Optional<ResultFormat> byMimeType(String mimeType) {
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
    public static List<ResultFormat> all() {
        return List.of(CSV, TSV, JSON, XML);
    }
}
