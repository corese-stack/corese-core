package fr.inria.corese.core.next.api.io.serialization;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.base.io.RDFFormat;

/**
 * Factory interface for creating {@link RDFSerializer} instances.
 * This interface defines a contract for classes that are responsible
 * for providing appropriate RDF serializers based on the desired
 * {@link RDFFormat}, a {@link Model} to be serialized, and
 * {@link SerializationOption}.
 * Implementations of this factory can manage the instantiation
 * and configuration of various RDF serializers, promoting
 * loose coupling and extensibility in the serialization process.
 */
public interface SerializerFactory {

    /**
     * Creates a new RDF serializer for the specified format and model.
     * 
     * @param format The {@link RDFFormat} to use for serialization.
     * @param model  The {@link Model} to be serialized.
     * @param config The {@link SerializationOption} configuration to use for
     *               serialization.
     * @return A new instance of an RDF serializer for the specified format and
     *         model.
     */
    RDFSerializer createSerializer(RDFFormat format, Model model, SerializationOption config);
}
