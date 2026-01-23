package fr.inria.corese.core.next.api.sparql.io.serializer;

import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.next.api.io.IOOptions;

/**
 * Factory interface for creating {@link ResultSerializer} instances. This interface defines functions to create serializer based on the given result {@link fr.inria.corese.core.kgram.core.Mappings} and the desired {@link fr.inria.corese.core.next.api.base.io.ResultFormat}
 */
public interface ResultSerializerFactory {

    /**
     * Creates a serializer for the given {@link fr.inria.corese.core.kgram.core.Mappings} results in the given {@link fr.inria.corese.core.next.api.base.io.ResultFormat format}
     * @param format The {@link fr.inria.corese.core.next.api.base.io.ResultFormat} to use for serialization.
     * @param results The {@link fr.inria.corese.core.kgram.core.Mappings} results to be serialized
     * @return a new instance of {@link ResultSerializer} with default configuration.
     */
    ResultSerializer createSerializer(ResultSerializer format, Mappings results);

    /**
     * Creates a serializer for the given {@link fr.inria.corese.core.kgram.core.Mappings} results in the given {@link fr.inria.corese.core.next.api.base.io.ResultFormat format}
     * @param format The {@link fr.inria.corese.core.next.api.base.io.ResultFormat} to use for serialization.
     * @param results The {@link fr.inria.corese.core.kgram.core.Mappings} results to be serialized
     * @param options Options to configure the serialization
     * @return a new instance of {@link ResultSerializer} with default configuration.
     */
    ResultSerializer createSerializer(ResultSerializer format, Mappings results, IOOptions options);
}
