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


    RDFSerializer createSerializer(RDFFormat format, Model model, SerializationOption config);
}
