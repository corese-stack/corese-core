package fr.inria.corese.core.next.api.parser;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public enum RDFFormats implements RDFFormat {

    TURTLE("Turtle",
           List.of("text/turtle"),
           List.of("ttl"),
           true,
           false,
           false),
    N3("N3",
            List.of("text/n3"),
           List.of("n3"),
           true,
           false,
           false),
    RDF_XML("RDF/XML",
            List.of("application/rdf+xml"),
            List.of("rdf", "xml"),
            true,
            false,
            false),
    JSON_LD("JSON-LD",
            List.of("application/ld+json"),
            List.of("jsonld", "json"),
            true,
            true,
            false),
    N_TRIPLES("N-Triples",
            List.of("application/n-triples"),
            List.of("nt"),
            false,
            false,
            false),
    TRIG("TriG",
            List.of("application/trig"),
            List.of("trig"),
            true,
            true,
            false),
    NQUADS("N-Quads",
            List.of("application/n-quads"),
            List.of("nq"),
            true,
            true,
            false);

    private static final boolean DEFAULT_SUPPORTS_NAMESPACES = true;
    private static final boolean DEFAULT_SUPPORTS_CONTEXTS = true;
    private static final boolean DEFAULT_SUPPORTS_RDF_STAR = false;

    /**
     * The file format human-readable name.
     */
    private final String name;

    /**
     * The file format's MIME types. The first item in the list is interpreted as the default MIME type for the format.
     */
    private final List<String> mimeTypes;

    /**
     * The file format's (default) charset.
     */
    private final Charset charset;

    /**
     * The file format's file extensions. The first item in the list is interpreted as the default file extension for
     * the format.
     */
    private final List<String> fileExtensions;

    /**
     * Flag indicating whether the RDFFormat can encode namespace information.
     */
    private final boolean supportsNamespaces;

    /**
     * Flag indicating whether the RDFFormat can encode context information (ex: Graphs or quads).
     */
    private final boolean supportsContexts;

    /**
     * Flag indicating whether the RDFFormat can encode RDF-star triples natively.
     */
    private final boolean supportsRDFStar;

    RDFFormats(String name,
               List<String> mimeTypes,
               Charset charset,
               List<String> fileExtensions,
               boolean supportsNamespaces,
               boolean supportsContexts,
               boolean supportsRDFStar) {
        this.name = name;
        this.mimeTypes = mimeTypes;
        this.charset = charset;
        this.fileExtensions = fileExtensions;
        this.supportsNamespaces = supportsNamespaces;
        this.supportsContexts = supportsContexts;
        this.supportsRDFStar = supportsRDFStar;
    }

    RDFFormats(String name,
               List<String> mimeTypes,
               Charset charset,
               List<String> fileExtensions) {
        this(name, mimeTypes, charset, fileExtensions, DEFAULT_SUPPORTS_NAMESPACES, DEFAULT_SUPPORTS_CONTEXTS, DEFAULT_SUPPORTS_RDF_STAR);
    }

    RDFFormats(String name,
               List<String> mimeTypes,
               List<String> fileExtensions) {
        this(name, mimeTypes, StandardCharsets.UTF_8, fileExtensions, DEFAULT_SUPPORTS_NAMESPACES, DEFAULT_SUPPORTS_CONTEXTS, DEFAULT_SUPPORTS_RDF_STAR);
    }

    RDFFormats(String name,
               List<String> mimeTypes,
               List<String> fileExtensions,
               boolean supportsNamespaces,
               boolean supportsContexts,
               boolean supportsRDFStar) {
        this(name, mimeTypes, StandardCharsets.UTF_8, fileExtensions, supportsNamespaces, supportsContexts, supportsRDFStar);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDefaultMIMEType() {
        return mimeTypes.get(0);
    }

    @Override
    public boolean hasDefaultMIMEType(String mimeType) {
        return getDefaultMIMEType().equalsIgnoreCase(mimeType);
    }

    @Override
    public List<String> getMIMETypes() {
        return Collections.unmodifiableList(mimeTypes);
    }

    @Override
    public boolean hasMIMEType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        String type = mimeType;
        if (mimeType.indexOf(';') > 0) {
            type = mimeType.substring(0, mimeType.indexOf(';'));
        }
        for (String mt : this.mimeTypes) {
            if (mt.equalsIgnoreCase(mimeType)) {
                return true;
            }
            if (mimeType != type && mt.equalsIgnoreCase(type)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getDefaultFileExtension() {
        if (fileExtensions.isEmpty()) {
            return null;
        } else {
            return fileExtensions.get(0);
        }
    }

    @Override
    public boolean hasDefaultFileExtension(String extension) {
        String ext = getDefaultFileExtension();
        return ext != null && ext.equalsIgnoreCase(extension);
    }

    @Override
    public List<String> getFileExtensions() {
        return Collections.unmodifiableList(fileExtensions);
    }

    @Override
    public boolean hasFileExtension(String extension) {
        for (String ext : fileExtensions) {
            if (ext.equalsIgnoreCase(extension)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    @Override
    public boolean hasCharset() {
        return charset != null;
    }

    @Override
    public boolean supportsNamespaces() {
        return supportsNamespaces;
    }

    @Override
    public boolean supportsContexts() {
        return supportsContexts;
    }

    @Override
    public boolean supportsRDFStar() {
        return supportsRDFStar;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);

        sb.append(name);

        sb.append(" (mimeTypes=");
        for (int i = 0; i < mimeTypes.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(mimeTypes.get(i));
        }

        sb.append("; ext=");
        for (int i = 0; i < fileExtensions.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(fileExtensions.get(i));
        }

        sb.append(")");

        return sb.toString();
    }

}
