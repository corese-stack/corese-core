package fr.inria.corese.core.next.api.parser;

import java.nio.charset.Charset;
import java.util.*;
import fr.inria.corese.core.next.api.IRI;

public interface RDFFormat {

    /**
     * Gets the name of this file format.
     *
     * @return A human-readable format name, e.g. "PLAIN TEXT".
     */
    String getName();


    /**
     * Gets the default MIME type for this file format.
     *
     * @return A MIME type string, e.g. "text/plain".
     */
    String getDefaultMIMEType() ;


    /**
     * Checks if the specified MIME type matches the FileFormat's default MIME type. The MIME types are compared
     * ignoring upper/lower-case differences.
     *
     * @param mimeType The MIME type to compare to the FileFormat's default MIME type.
     * @return <var>true</var> if the specified MIME type matches the FileFormat's default MIME type.
     */
    boolean hasDefaultMIMEType(String mimeType);

    /**
     * Gets the file format's MIME types.
     *
     * @return An unmodifiable list of MIME type strings, e.g. "text/plain".
     */
    List<String> getMIMETypes();



    /**
     * Checks if specified MIME type matches one of the FileFormat's MIME types. The MIME types are compared ignoring
     * upper/lower-case differences.
     *
     * @param mimeType The MIME type to compare to the FileFormat's MIME types.
     * @return <var>true</var> if the specified MIME type matches one of the FileFormat's MIME types.
     */
    boolean hasMIMEType(String mimeType);

    /**
     * Gets the default file name extension for this file format.
     *
     * @return A file name extension (excluding the dot), e.g. "txt", or <var>null</var> if there is no common file
     *         extension for the format.
     */
    String getDefaultFileExtension();

    /**
     * Checks if the specified file name extension matches the FileFormat's default file name extension. The file name
     * extension MIME types are compared ignoring upper/lower-case differences.
     *
     * @param extension The file extension to compare to the FileFormat's file extension.
     * @return <var>true</var> if the file format has a default file name extension and if it matches the specified
     *         extension, <var>false</var> otherwise.
     */
    boolean hasDefaultFileExtension(String extension);

    /**
     * Gets the file format's file extensions.
     *
     * @return An unmodifiable list of file extension strings, e.g. "txt".
     */
    List<String> getFileExtensions();

    /**
     * Checks if the FileFormat's file extension is equal to the specified file extension. The file extensions are
     * compared ignoring upper/lower-case differences.
     *
     * @param extension The file extension to compare to the FileFormat's file extension.
     * @return <var>true</var> if the specified file extension is equal to the FileFormat's file extension.
     */
    boolean hasFileExtension(String extension);

    /**
     * Get the (default) charset for this file format.
     *
     * @return the (default) charset for this file format, or null if this format does not have a default charset.
     */
    Charset getCharset();

    /**
     * Checks if the FileFormat has a (default) charset.
     *
     * @return <var>true</var> if the FileFormat has a (default) charset.
     */
    boolean hasCharset();

    /**
     * Return <var>true</var> if the RDFFormat supports the encoding of namespace/prefix information.
     */
    boolean supportsNamespaces();

    /**
     * Return <var>true</var> if the RDFFormat supports the encoding of contexts/named graphs.
     */
    boolean supportsContexts();

    /**
     * Return <var>true</var> if the RDFFormat supports the encoding of RDF-star triples natively.
     */
    boolean supportsRDFStar();
}
