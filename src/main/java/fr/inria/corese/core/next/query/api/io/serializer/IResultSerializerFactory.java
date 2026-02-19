package fr.inria.corese.core.next.query.api.io.serializer;

import fr.inria.corese.core.next.data.api.io.IOOptions;
import fr.inria.corese.core.next.data.api.io.serializer.Serializer;
import fr.inria.corese.core.next.query.api.base.io.ResultFormat;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;

/**
 * Factory interface for creating {@link ResultSerializer} instances. This interface defines functions to create serializer based on the given result {@link fr.inria.corese.core.kgram.core.Mappings} and the desired {@link ResultFormat}
 */
public interface IResultSerializerFactory {

    /**
     * Creates a serializer for the given {@link TupleQueryResult} results in the given {@link ResultFormat format}
     * @param format The {@link ResultFormat} to use for serialization.
     * @param results The {@link TupleQueryResult} results to be serialized
     * @return a new instance of {@link ResultSerializer} with default configuration.
     */
    ResultSerializer createSerializer(ResultFormat format, TupleQueryResult results);

    /**
     * Creates a serializer for the given boolean result in the given {@link ResultFormat format}
     *
     * @param format  The {@link ResultFormat} to use for serialization.
     * @param results The boolean result to be serialized
     * @return a new instance of {@link ResultSerializer} with default configuration.
     */
    Serializer createBooleanSerializer(ResultFormat format, boolean results);

    /**
     * Creates a serializer for the given {@link TupleQueryResult} results in the given {@link ResultFormat format}
     * @param format The {@link ResultFormat} to use for serialization.
     * @param results The {@link TupleQueryResult} results to be serialized
     * @param options Options to configure the serialization
     * @return a new instance of {@link ResultSerializer} with default configuration.
     */
    ResultSerializer createSerializer(ResultFormat format, TupleQueryResult results, IOOptions options);

    /**
     * Creates a serializer for the given boolean result in the given {@link ResultFormat format}
     *
     * @param format  The {@link ResultFormat} to use for serialization.
     * @param results The boolean result to be serialized
     * @param options Options to configure the serialization
     * @return a new instance of {@link ResultSerializer} with default configuration.
     */
    Serializer createBooleanSerializer(ResultFormat format, boolean results, IOOptions options);
}
