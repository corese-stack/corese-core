package fr.inria.corese.core.next.api.base.model.serialization;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.next.api.base.exception.SerializationException;
import fr.inria.corese.core.print.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Utility class for serializing RDF data to various formats.
 * Supports Turtle, JSON-LD, RDF/XML, N-Triples, and N-Quads formats.
 */
public class RdfSerializationUtil {

    private static final Logger logger = LoggerFactory.getLogger(RdfSerializationUtil.class);

    public enum Format {
        TURTLE, JSONLD, RDFXML, NTRIPLES, NQUADS
    }

    private static final Map<String, Format> FORMAT_MAP = initFormatMap();

    /**
     * Initializes format mapping
     */
    private static Map<String, Format> initFormatMap() {
        Map<String, Format> map = new HashMap<>();

        // Turtle formats
        map.put("turtle", Format.TURTLE);
        map.put("ttl", Format.TURTLE);

        // JSON-LD formats
        map.put("jsonld", Format.JSONLD);
        map.put("json-ld", Format.JSONLD);

        // RDF/XML formats
        map.put("rdfxml", Format.RDFXML);
        map.put("xml", Format.RDFXML);

        // N-Triples formats
        map.put("ntriples", Format.NTRIPLES);
        map.put("nt", Format.NTRIPLES);

        // N-Quads formats
        map.put("nquads", Format.NQUADS);
        map.put("nq", Format.NQUADS);

        return Collections.unmodifiableMap(map);
    }

    /**
     * Returns supported formats
     */
    public static Set<String> getSupportedFormats() {
        return FORMAT_MAP.keySet();
    }

    /**
     * Gets format from string
     */
    public static Format getFormat(String formatString) {
        Objects.requireNonNull(formatString, "Format string cannot be null");
        Format format = FORMAT_MAP.get(formatString.toLowerCase());
        if (format == null) {
            throw new IllegalArgumentException("Unsupported format: " + formatString +
                    ". Supported formats: " + getSupportedFormats());
        }
        return format;
    }



    /**
     * Main serialization method
     */
    public static void serialize(Graph graph, OutputStream out, String formatString)
            throws SerializationException {
        try {
            Objects.requireNonNull(graph, "Graph cannot be null");
            Objects.requireNonNull(out, "OutputStream cannot be null");

            Format format = getFormat(formatString);

            try (OutputStream bos = new BufferedOutputStream(out)) {
                switch (format) {
                    case NTRIPLES -> serializeToNTriples(graph, bos);
                    case NQUADS -> serializeToNQuads(graph, bos);
                    default -> throw new IllegalStateException("Unhandled format: " + format);
                }
            }
        } catch (IOException e) {
            logger.error("Serialization failed for format: {}", formatString, e);
            throw new SerializationException(
                    String.format("Failed to serialize graph to %s", formatString), e);
        }
    }


    // Serialization methods for each format
    public static void serializeToNTriples(Graph graph, OutputStream out) throws SerializationException {
        try {
            NTriplesFormat.create(graph).write(out);
        } catch (IOException e) {
            throw new SerializationException("NTriples serialization failed", e);
        }
    }

    public static void serializeToNQuads(Graph graph, OutputStream out) throws SerializationException {

        try {
            NQuadsFormat.create(graph).write(out);
        } catch (IOException e) {
            throw new SerializationException("NQuads serialization failed", e);
        }
    }
}