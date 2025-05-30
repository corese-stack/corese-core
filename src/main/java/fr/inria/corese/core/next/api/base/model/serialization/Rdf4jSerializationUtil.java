package fr.inria.corese.core.next.api.base.model.serialization;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

import java.io.OutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Utility class for serializing RDF4J {@link Model} objects to various RDF formats.
 * This class provides static methods to write RDF data to an {@link OutputStream}
 * in formats such as Turtle, JSON-LD, RDF/XML, and N-Triples, leveraging the RDF4J Rio API.
 * It handles common serialization configurations like pretty-printing and
 * provides dynamic format selection based on string identifiers.
 */
public class Rdf4jSerializationUtil {

    /**
     * A unmodifiable map to quickly look up {@link RDFFormat} instances
     * based on common string identifiers (case-insensitive).
     */
    private static final Map<String, RDFFormat> FORMAT_MAP = initFormatMap();

    /**
     * The default {@link WriterConfig} used for serialization,
     * configured for pretty-printing.
     */
    private static final WriterConfig DEFAULT_CONFIG = createDefaultConfig();

    /**
     * Initializes and returns an unmodifiable map of supported RDF format strings to their
     * corresponding {@link RDFFormat} objects.
     *
     * @return An unmodifiable {@link Map} containing format string to {@link RDFFormat} mappings.
     */
    private static Map<String, RDFFormat> initFormatMap() {
        Map<String, RDFFormat> map = new HashMap<>();

        // Common file extension for Turtle
        map.put("turtle", RDFFormat.TURTLE);
        map.put("ttl", RDFFormat.TURTLE);

        // Common alternative for JSON-LD
        map.put("jsonld", RDFFormat.JSONLD);
        map.put("json-ld", RDFFormat.JSONLD);

        // Common alternative for RDF/XML
        map.put("rdfxml", RDFFormat.RDFXML);
        map.put("xml", RDFFormat.RDFXML);

        return Collections.unmodifiableMap(map);
    }

    /**
     * Creates and returns a default {@link WriterConfig} instance.
     * This configuration enables pretty-printing for human-readable output.
     * Deprecated JSON-LD specific settings have been removed.
     *
     * @return A {@link WriterConfig} instance with default serialization settings.
     */
    private static WriterConfig createDefaultConfig() {
        WriterConfig config = new WriterConfig();

        config.set(BasicWriterSettings.PRETTY_PRINT, true);


        return config;
    }

    /**
     * Returns a set of string identifiers for all supported RDF serialization formats.
     * These identifiers can be used with the {@link #serialize(Model, OutputStream, String)}
     * and {@link #getRdfFormat(String)} methods.
     *
     * @return A {@link Set} of {@link String} representing the supported RDF format identifiers.
     */
    public static Set<String> getSupportedFormats() {
        return FORMAT_MAP.keySet();
    }

    /**
     * Serializes an RDF4J {@link Model} to the specified {@link OutputStream} in Turtle format.
     * The serialization uses the default writer configuration (e.g., pretty-printing enabled).
     *
     * @param model        The RDF4J {@link Model} to serialize. Must not be {@code null}.
     * @param outputStream The {@link OutputStream} to write the serialized data to. Must not be {@code null}.
     * @throws IOException          If an I/O error occurs during serialization.
     * @throws NullPointerException If {@code model} or {@code outputStream} is {@code null}.
     * @see #serialize(Model, OutputStream, RDFFormat)
     */
    public static void serializeToTurtle(Model model, OutputStream outputStream) throws IOException {
        serialize(model, outputStream, RDFFormat.TURTLE);
    }

    /**
     * Serializes an RDF4J {@link Model} to the specified {@link OutputStream} in JSON-LD format.
     * The serialization uses the default writer configuration (e.g., pretty-printing enabled).
     *
     * @param model        The RDF4J {@link Model} to serialize. Must not be {@code null}.
     * @param outputStream The {@link OutputStream} to write the serialized data to. Must not be {@code null}.
     * @throws IOException          If an I/O error occurs during serialization.
     * @throws NullPointerException If {@code model} or {@code outputStream} is {@code null}.
     * @see #serialize(Model, OutputStream, RDFFormat)
     */
    public static void serializeToJsonLd(Model model, OutputStream outputStream) throws IOException {
        serialize(model, outputStream, RDFFormat.JSONLD);
    }

    /**
     * Serializes an RDF4J {@link Model} to the specified {@link OutputStream} in RDF/XML format.
     * The serialization uses the default writer configuration (e.g., pretty-printing enabled).
     *
     * @param model        The RDF4J {@link Model} to serialize. Must not be {@code null}.
     * @param outputStream The {@link OutputStream} to write the serialized data to. Must not be {@code null}.
     * @throws IOException          If an I/O error occurs during serialization.
     * @throws NullPointerException If {@code model} or {@code outputStream} is {@code null}.
     * @see #serialize(Model, OutputStream, RDFFormat)
     */
    public static void serializeToRdfXml(Model model, OutputStream outputStream) throws IOException {
        serialize(model, outputStream, RDFFormat.RDFXML);
    }


    /**
     * Resolves an {@link RDFFormat} from a given string identifier.
     * The lookup is case-insensitive.
     *
     * @param formatString The string representing the desired RDF format (e.g., "turtle", "jsonld", "rdfxml", "ntriples").
     *                     Must not be {@code null}.
     * @return The corresponding {@link RDFFormat} enum.
     * @throws IllegalArgumentException If the provided {@code formatString} is not recognized as a supported format.
     * @throws NullPointerException     If {@code formatString} is {@code null}.
     */
    public static RDFFormat getRdfFormat(String formatString) throws IllegalArgumentException {
        Objects.requireNonNull(formatString, "Format string cannot be null");
        RDFFormat format = FORMAT_MAP.get(formatString.toLowerCase());
        if (format == null) {
            throw new IllegalArgumentException("Unsupported format: " + formatString +
                    ". Supported formats: " + getSupportedFormats());
        }
        return format;
    }

    /**
     * Serializes an RDF4J {@link Model} to a specified {@link OutputStream},
     * with the format determined dynamically by a string identifier.
     * This method is useful when the target format is known at runtime.
     *
     * @param model        The RDF4J {@link Model} to serialize. Must not be {@code null}.
     * @param outputStream The {@link OutputStream} to write the serialized data to. Must not be {@code null}.
     * @param formatString The string identifier of the desired RDF format (e.g., "turtle", "jsonld", "rdfxml", "ntriples").
     *                     Must not be {@code null}.
     * @throws IOException              If an I/O error occurs during serialization or if the underlying
     *                                  {@link Rio #write(Model, OutputStream, RDFFormat, WriterConfig)} call fails.
     * @throws IllegalArgumentException If the provided {@code formatString} is not recognized.
     * @throws NullPointerException     If {@code model}, {@code outputStream}, or {@code formatString} is {@code null}.
     */
    public static void serialize(Model model, OutputStream outputStream, String formatString) throws IOException {
        Objects.requireNonNull(model, "Model cannot be null");
        Objects.requireNonNull(outputStream, "OutputStream cannot be null");

        RDFFormat format = getRdfFormat(formatString);
        serialize(model, outputStream, format);
    }

    /**
     * Internal method to perform the actual serialization using RDF4J Rio.
     * This method applies the {@link #DEFAULT_CONFIG}.
     *
     * @param model        The RDF4J {@link Model} to serialize.
     * @param outputStream The {@link OutputStream} to write the serialized data to.
     * @param format       The {@link RDFFormat} to use for serialization.
     * @throws IOException If an I/O error occurs during serialization.
     */
    private static void serialize(Model model, OutputStream outputStream, RDFFormat format) throws IOException {
        try {
            Rio.write(model, outputStream, format, DEFAULT_CONFIG);
        } catch (Exception e) {

            throw new IOException("Failed to serialize RDF model to " + format.getName(), e);
        }
    }

    /**
     * Creates a custom {@link WriterConfig} instance with a specified pretty-printing setting.
     * This method can be used if a serialization operation requires different settings
     * than the {@link #DEFAULT_CONFIG}.
     *
     * @param prettyPrint A boolean indicating whether pretty-printing should be enabled.
     * @return A new {@link WriterConfig} instance configured with the specified pretty-printing setting.
     */
    public static WriterConfig createCustomConfig(boolean prettyPrint) {
        WriterConfig config = new WriterConfig();
        config.set(BasicWriterSettings.PRETTY_PRINT, prettyPrint);
        return config;
    }
}
