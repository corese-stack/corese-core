package fr.inria.corese.core.next.query.api.io.format;

import fr.inria.corese.core.next.data.api.io.format.FileFormat;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Describes the standard SPARQL result serialization formats.
 */
public final class ResultFormat extends FileFormat {

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
            List.of("srj", "json"),
            List.of("application/sparql-results+json")
    );

    public static final ResultFormat XML = new ResultFormat(
            "XML",
            List.of("srx", "xml"),
            List.of("application/sparql-results+xml")
    );

    /**
     * Constructs a new ResultFormat instance.
     *
     * @param name       The human-readable name of the format.
     * @param extensions The list of file extensions.
     * @param mimeTypes  The list of MIME types.
     * @throws NullPointerException if name, extensions or mimeTypes is null
     * @throws IllegalArgumentException if extensions or mimeTypes is empty
     */
    public ResultFormat(
            String name,
            List<String> extensions,
            List<String> mimeTypes) {
        super(name, extensions, mimeTypes);
    }

    /**
     * Returns all known SPARQL result formats.
     *
     * @return an unmodifiable list of result formats
     */
    public static List<ResultFormat> all() {
        return List.of(CSV, TSV, JSON, XML);
    }

    /** Finds a standard result format by name, ignoring case. */
    public static Optional<ResultFormat> byName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return all().stream()
                .filter(format -> format.getName().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    /** Finds a standard result format by extension, with or without a leading dot. */
    public static Optional<ResultFormat> byExtension(String extension) {
        if (extension == null) {
            return Optional.empty();
        }
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        String candidate = normalized;
        return all().stream()
                .filter(format -> format.getExtensions().stream()
                        .anyMatch(value -> value.equalsIgnoreCase(candidate)))
                .findFirst();
    }

    /** Finds a standard result format by MIME type, ignoring media-type parameters. */
    public static Optional<ResultFormat> byMimeType(String mimeType) {
        if (mimeType == null) {
            return Optional.empty();
        }
        String normalized = mimeType.split(";", 2)[0].trim();
        return all().stream()
                .filter(format -> format.getMimeTypes().stream()
                        .anyMatch(value -> value.equalsIgnoreCase(normalized)))
                .findFirst();
    }
}
