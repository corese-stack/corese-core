package fr.inria.corese.core.next.data.io.serializer;

import java.io.Writer;

import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.base.io.RDFFormat;
import fr.inria.corese.core.next.data.impl.exception.SerializationException;

/**
 * Interface for Serializer of {@link Model} instances to a specified {@link RDFFormat}.
 * @see {@link SerializerFactory}.
 */
public interface RDFSerializer extends Serializer {

}