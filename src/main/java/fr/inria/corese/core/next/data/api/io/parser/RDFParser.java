package fr.inria.corese.core.next.data.api.io.parser;

import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.option.RDFParsingOptions;
import fr.inria.corese.core.next.data.api.exception.ParsingException;

import java.io.InputStream;
import java.io.Reader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Parses RDF data from an {@link InputStream} or {@link Reader} into a model.
 * Caller-owned input sources are never closed by the parser.
 *
 * @see RDFParserFactory
 * @see RDFFormat
 */
public interface RDFParser {

    /**
     * Gets the RDF format that this parser can parse.
     */
    RDFFormat getRDFFormat();

    void setConfig(RDFParsingOptions config);
    RDFParsingOptions getConfig();

    /**
     * Parses RDF data from the specified InputStream or Reader and adds it to the model.
     *
     * @param in      The InputStream to read RDF data from.
     * @throws ParsingException if an error occurs during parsing
     */
    void parse(InputStream in) throws ParsingException;

    /**
     * Parses RDF data from the specified InputStream or Reader and adds it to the model.
     *
     * @param in      The InputStream to read RDF data from.
     * @param baseURI The base URI for resolving relative URIs in the RDF data.
     * @throws ParsingException if an error occurs during parsing
     */
    void parse(InputStream in, String baseURI) throws ParsingException;

    /**
     * Parses RDF data from the specified InputStream or Reader and adds it to the model.
     *
     * @param reader  The Reader to read RDF data from.
     * @throws ParsingException if an error occurs during parsing
     */
    void parse(Reader reader) throws ParsingException;

    /**
     * Parses RDF data from the specified InputStream or Reader and adds it to the model.
     *
     * @param reader  The Reader to read RDF data from.
     * @param baseURI The base URI for resolving relative URIs in the RDF data.
     * @throws ParsingException if an error occurs during parsing
     */
    void parse(Reader reader, String baseURI) throws ParsingException;

    /**
     * Reads UTF-8 RDF from a file, using the file URI as the base IRI, and
     * closes the file opened by this method.
     */
    default void parse(Path path) throws ParsingException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            parse(reader, path.toAbsolutePath().normalize().toUri().toString());
        } catch (IOException e) {
            throw new ParsingException("Could not read RDF from " + path, e);
        }
    }

    /** Reads UTF-8 RDF from a file using an explicit base IRI. */
    default void parse(Path path, String baseURI) throws ParsingException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            parse(reader, baseURI);
        } catch (IOException e) {
            throw new ParsingException("Could not read RDF from " + path, e);
        }
    }
}
