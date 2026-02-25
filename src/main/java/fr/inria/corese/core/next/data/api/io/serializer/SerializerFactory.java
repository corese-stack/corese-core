package fr.inria.corese.core.next.data.api.io.serializer;

import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.base.io.RDFFormat;
import fr.inria.corese.core.next.data.api.io.IOOptions;

/**
 * Factory interface for creating {@link RDFSerializer} instances.
 * This interface defines a contract for classes that are responsible
 * for providing appropriate RDF serializers based on the desired
 * {@link RDFFormat}, a {@link Model} to be serialized, and
 * {@link IOOptions}.
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
     * @param config The {@link IOOptions} configuration to use for
     *               serialization.
     * @return A new instance of an RDF serializer for the specified format and
     *         model.
     */
    RDFSerializer createSerializer(RDFFormat format, Model model, IOOptions config);

    /**
     * Creates a new RDF serializer for the specified format and model
     * using the default configuration for that format.
     *
     * @param format The {@link RDFFormat} to use for serialization.
     * @param model  The {@link Model} to be serialized.
     * @return A new instance of an RDF serializer for the specified format and
     *         model with default configuration.
     */
    RDFSerializer createSerializer(RDFFormat format, Model model);
}