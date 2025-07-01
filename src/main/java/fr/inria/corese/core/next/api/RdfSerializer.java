package fr.inria.corese.core.next.api;

import fr.inria.corese.core.next.impl.exception.SerializationException;

import java.io.Writer;

/**
 * Factory interface for creating {@link RdfSerializer} instances.
 * This interface defines a contract for classes that are responsible
 * for providing appropriate RDF serializers based on the desired
 * {@link fr.inria.corese.core.next.impl.common.serialization.RdfFormat}, a {@link Model} to be serialized, and
 * {@link SerializationConfig}.
 * Implementations of this factory can manage the instantiation
 * and configuration of various RDF serializers, promoting
 * loose coupling and extensibility in the serialization process.
 */
public interface RdfSerializer {

    /**
     * A serializer that converts a {@link Model} instance
     * into a specific output format and writes it to a character stream.
     * Implementations may follow standard RDF serialization formats
     * (e.g., Turtle, N-Triples, JSON-LD, TriG , XML ), or define custom formats.
     *
     * @param writer the destination {@link Writer} for the serialized
     *               output
     * @throws SerializationException if an error occurs during the serialization
     *                                process
     */
    void write(Writer writer) throws SerializationException;
}