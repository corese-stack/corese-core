package fr.inria.corese.core.next.data.api.io.serializer;

import fr.inria.corese.core.next.data.api.io.format.FileFormat;
import fr.inria.corese.core.next.data.api.exception.SerializationException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Interface for all serializers (RDF models or SPARQL results) to a specified
 * {@link FileFormat}.
 *
 * <p>A serializer consumes its source once. It never closes the caller-owned
 * {@link Writer}; callers remain responsible for closing it. Implementations
 * flush any internal buffers before {@link #write(Writer)} returns.</p>
 */
public interface Serializer {

    /**
     * A serializer that converts an object into a specific output format and writes it to a character stream.
     *
     * @param writer the destination {@link Writer} for the serialized
     *               output
     * @throws SerializationException if an error occurs during the serialization process
     */
    void write(final Writer writer) throws SerializationException;

    /**
     * Writes UTF-8 text to a caller-owned byte stream without closing it.
     */
    default void write(OutputStream output) throws SerializationException {
        OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        write(writer);
        try {
            writer.flush();
        } catch (IOException e) {
            throw new SerializationException("Could not flush serialized output", getFormat(), e);
        }
    }

    /**
     * Writes UTF-8 text to a file. The serializer owns and closes the file it opens.
     */
    default void write(Path path) throws SerializationException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            write(writer);
        } catch (IOException e) {
            throw new SerializationException("Could not write serialized output to " + path, getFormat(), e);
        }
    }

    /**
     * Serializes the source to a string.
     *
     * <p>This convenience method keeps the complete serialized representation in
     * memory. Prefer {@link #write(Writer)} for large outputs.</p>
     *
     * @return the serialized representation
     * @throws SerializationException if serialization fails
     */
    default String writeToString() throws SerializationException {
        StringWriter writer = new StringWriter();
        write(writer);
        return writer.toString();
    }

    /**
     * Returns the format name for error messages and logging.
     *
     * @return the format name (e.g., "XML", "Turtle").
     */
    default String getFormatName() {
        return getFormat().getName();
    }

    FileFormat getFormat();
}
