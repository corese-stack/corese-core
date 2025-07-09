package fr.inria.corese.core.next.api;

import fr.inria.corese.core.next.impl.common.serialization.RdfFormat;

/**
 * Factory interface for creating {@link RdfSerializer} instances.
 * This interface defines a contract for classes that are responsible
 * for providing appropriate RDF serializers based on the desired
 * {@link RdfFormat}, a {@link Model} to be serialized, and
 * {@link SerializationConfig}.
 * Implementations of this factory can manage the instantiation
 * and configuration of various RDF serializers, promoting
 * loose coupling and extensibility in the serialization process.
 */
public interface SerializerFactory {


    RdfSerializer createSerializer(RdfFormat format, Model model, SerializationConfig config);
}
