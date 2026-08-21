package fr.inria.corese.core.next.query.api.io;

import fr.inria.corese.core.next.data.api.io.format.FileFormat;

import java.util.List;

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
     * Returns all known SPARQL result formats.
     *
     * @return an unmodifiable list of result formats
     */
    public static List<ResultFormat> all() {
        return List.of(CSV, TSV, JSON, XML);
    }
}
