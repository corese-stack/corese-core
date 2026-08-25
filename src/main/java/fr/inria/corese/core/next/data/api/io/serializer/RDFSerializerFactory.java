package fr.inria.corese.core.next.data.api.io.serializer;

import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.option.RDFSerializationOptions;

/**
 * Factory interface for creating {@link RDFSerializer} instances.
 * This interface defines a contract for classes that are responsible
 * for providing appropriate RDF serializers based on the desired
 * {@link RDFFormat}, a {@link Model} to be serialized, and
 * {@link RDFSerializationOptions}. Implementations reject options belonging to another
 * format rather than silently ignoring them.
 * Implementations of this factory can manage the instantiation
 * and configuration of various RDF serializers, promoting
 * loose coupling and extensibility in the serialization process.
 */
public interface RDFSerializerFactory {

    /**
     * Creates a new RDF serializer for the specified format and model.
     *
     * @param format The {@link RDFFormat} to use for serialization.
     * @param model  The {@link Model} to be serialized.
     * @param config The {@link RDFSerializationOptions} configuration to use for
     *               serialization.
     * @return A new instance of an RDF serializer for the specified format and
     *         model.
     */
    RDFSerializer createSerializer(RDFFormat format, Model model, RDFSerializationOptions config);

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

    /**
     * Creates a serializer for a statement source such as a graph query result.
     * Line-oriented formats can be written progressively; formats requiring
     * global graph analysis may materialize the source internally.
     */
    RDFSerializer createSerializer(RDFFormat format, Iterable<Statement> statements);

    /** Creates a configured serializer for a statement source. */
    RDFSerializer createSerializer(
            RDFFormat format,
            Iterable<Statement> statements,
            RDFSerializationOptions config);
}
