package fr.inria.corese.core.next.query.api.io.serializer;

import fr.inria.corese.core.next.query.api.io.ResultFormat;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;

/**
 * Creates serializers for SPARQL tuple and boolean results.
 *
 * <p>Serializers consume tuple results but do not close them. The caller owns
 * both the result and the destination writer.</p>
 */
public interface ResultSerializerFactory {

    /**
     * Creates a serializer for the given {@link TupleQueryResult} results in the given {@link ResultFormat format}
     * @param format The {@link ResultFormat} to use for serialization.
     * @param results The {@link TupleQueryResult} results to be serialized
     * @return a new instance of {@link ResultSerializer} with default configuration.
     */
    ResultSerializer createTupleSerializer(ResultFormat format, TupleQueryResult results);

    /**
     * Creates a serializer for the given boolean result in the given {@link ResultFormat format}
     *
     * @param format  The {@link ResultFormat} to use for serialization.
     * @param result The boolean result to be serialized
     * @return a new instance of {@link ResultSerializer} with default configuration.
     */
    BooleanResultSerializer createBooleanSerializer(ResultFormat format, boolean result);

    /**
     * Creates a serializer for the given {@link TupleQueryResult} results in the given {@link ResultFormat format}
     * @param format The {@link ResultFormat} to use for serialization.
     * @param results The {@link TupleQueryResult} results to be serialized
     * @param options SPARQL result options; implementations reject unrelated
     *                {@link ResultIOOptions} instead of silently ignoring them
     * @return a new instance of {@link ResultSerializer} configured with the given options.
     */
    ResultSerializer createTupleSerializer(
            ResultFormat format,
            TupleQueryResult results,
            ResultIOOptions options);

    /**
     * Creates a serializer for the given boolean result in the given {@link ResultFormat format}
     *
     * @param format  The {@link ResultFormat} to use for serialization.
     * @param result The boolean result to be serialized
     * @param options Options to configure the serialization
     * @return a new instance of {@link ResultSerializer} with default configuration.
     */
    BooleanResultSerializer createBooleanSerializer(
            ResultFormat format,
            boolean result,
            ResultIOOptions options);
}
