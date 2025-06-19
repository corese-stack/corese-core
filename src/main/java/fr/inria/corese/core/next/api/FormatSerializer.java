package fr.inria.corese.core.next.api;

import java.io.Writer;

import fr.inria.corese.core.next.impl.exception.SerializationException;

public interface FormatSerializer {

    /**
     * A serializer that converts a {@link Model} instance
     * into a specific output format and writes it to a character stream.
     *
     * Implementations may follow standard RDF serialization formats
     * (e.g., Turtle, N-Triples, JSON-LD), or define custom formats.
     *
     * @param writer the destination {@link Writer} for the serialized
     *               output
     * @throws SerializationException if an error occurs during the serialization
     *                                process
     */
    void write(Writer writer) throws SerializationException;
}