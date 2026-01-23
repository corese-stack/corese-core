package fr.inria.corese.core.next.api.io.serializer;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.base.io.FileFormat;
import fr.inria.corese.core.next.impl.exception.SerializationException;

import java.io.File;
import java.io.Writer;

/**
 * Interface for all serializers (RDF models or SPARQL results) to a specified {@link fr.inria.corese.core.next.api.base.io.FileFormat}
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
     * Returns the format name for error messages and logging.
     *
     * @return the format name (e.g., "XML", "Turtle").
     */
    default String getFormatName() {
        return getFormat().getName();
    }

    FileFormat getFormat();
}
