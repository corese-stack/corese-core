package fr.inria.corese.core.next.api.io.parser;

import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.io.IOOptions;

import java.io.InputStream;
import java.io.Reader;

/**
 * An interface for parsing RDF data from an InputStream or Reader and adding statements to a model.
 *
 * @see RDFParserFactory
 * @see RDFFormat
 */
public interface RDFParser {

    /**
     * Gets the RDF format that this parser can parse.
     */
    RDFFormat getRDFFormat();

    void setConfig(IOOptions config);
    IOOptions getConfig();

    /**
     * Parses RDF data from the specified InputStream or Reader and adds it to the model.
     *
     * @param in      The InputStream to read RDF data from.
     */
    void parse(InputStream in);

    /**
     * Parses RDF data from the specified InputStream or Reader and adds it to the model.
     *
     * @param in      The InputStream to read RDF data from.
     * @param baseURI The base URI for resolving relative URIs in the RDF data.
     */
    void parse(InputStream in, String baseURI);

    /**
     * Parses RDF data from the specified InputStream or Reader and adds it to the model.
     *
     * @param reader  The Reader to read RDF data from.
     */
    void parse(Reader reader);

    /**
     * Parses RDF data from the specified InputStream or Reader and adds it to the model.
     *
     * @param reader  The Reader to read RDF data from.
     * @param baseURI The base URI for resolving relative URIs in the RDF data.
     */
    void parse(Reader reader, String baseURI);
}
