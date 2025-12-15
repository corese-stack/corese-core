package fr.inria.corese.core.next.impl.io.serialization;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.serializer.RDFSerializer;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import fr.inria.corese.core.next.impl.io.common.JSONLDOptions;
import fr.inria.corese.core.next.impl.io.serialization.canonical.RDFC10Canonicalizer;
import fr.inria.corese.core.next.impl.io.serialization.canonical.RDFC10Serializer;
import fr.inria.corese.core.next.impl.io.serialization.canonical.RDFC10SerializerOptions;
import fr.inria.corese.core.next.impl.io.serialization.jsonld.JSONLDSerializer;
import fr.inria.corese.core.next.impl.io.serialization.nquads.NQuadsSerializer;
import fr.inria.corese.core.next.impl.io.serialization.nquads.NQuadsSerializerOptions;
import fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesSerializer;
import fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesSerializerOptions;
import fr.inria.corese.core.next.impl.io.serialization.rdfxml.RDFXMLSerializer;
import fr.inria.corese.core.next.impl.io.serialization.rdfxml.RDFXMLSerializerOptions;
import fr.inria.corese.core.next.impl.io.serialization.trig.TriGSerializer;
import fr.inria.corese.core.next.impl.io.serialization.trig.TriGSerializerOptions;
import fr.inria.corese.core.next.impl.io.serialization.turtle.TurtleSerializer;
import fr.inria.corese.core.next.impl.io.serialization.turtle.TurtleSerializerOptions;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Default implementation of {@link fr.inria.corese.core.next.api.io.serializer.SerializerFactory}.
 * This factory is responsible for creating instances of {@link RDFSerializer}
 * based on the requested {@link RDFFormat}. It uses a registry pattern
 * to map each format to its corresponding serializer constructor,
 * providing a flexible and extensible way to manage serializer instances.
 *
 * <p>
 * It adapts the generic {@link fr.inria.corese.core.next.api.io.IOOptions} provided to the specific
 * configuration type expected by each serializer in the hierarchy, with a
 * fallback
 * to default configurations if an incompatible type is provided.
 * </p>
 */
public class SerializerFactory implements fr.inria.corese.core.next.api.io.serializer.SerializerFactory {

    private final Map<RDFFormat, BiFunction<Model, IOOptions, RDFSerializer>> registry;
    private final Map<RDFFormat, Function<Model, RDFSerializer>> defaultRegistry;
    private final ValueFactory coreseValueFactory;

    /**
     * Constructs a {@code SerializerFactory} and populates its registry
     * with constructors for all known {@link RDFFormat} implementations.
     * Each constructor attempts to cast the generic {@link IOOptions} to
     * the
     * specific configuration type required by the serializer. If the cast is not
     * possible,
     * it falls back to the format's default configuration.
     */
    public SerializerFactory() {
        this.coreseValueFactory = new CoreseAdaptedValueFactory();

        Map<RDFFormat, BiFunction<Model, IOOptions, RDFSerializer>> tempRegistry = new HashMap<>();
        Map<RDFFormat, Function<Model, RDFSerializer>> tempDefaultRegistry = new HashMap<>();

        tempRegistry.put(RDFFormat.TURTLE, (model, genericConfig) -> {
            if (genericConfig instanceof TurtleSerializerOptions specificConfig) {
                return new TurtleSerializer(model, specificConfig);
            }
            throw new SerializationException(
                    "Invalid configuration type. Expected TurtleSerializerOptions, got: " +
                            genericConfig.getClass().getSimpleName(),
                    RDFFormat.TURTLE.getName()
            );
        });
        tempDefaultRegistry.put(RDFFormat.TURTLE, TurtleSerializer::new);

        tempRegistry.put(RDFFormat.NTRIPLES, (model, genericConfig) -> {
            if (genericConfig instanceof NTriplesSerializerOptions specificConfig) {
                return new NTriplesSerializer(model, specificConfig);
            }
            throw new SerializationException(
                    "Invalid configuration type. Expected NTriplesSerializerOptions, got: " +
                            genericConfig.getClass().getSimpleName(),
                    RDFFormat.NTRIPLES.getName()
            );
        });
        tempDefaultRegistry.put(RDFFormat.NTRIPLES, NTriplesSerializer::new);

        tempRegistry.put(RDFFormat.NQUADS, (model, genericConfig) -> {
            if (genericConfig instanceof NQuadsSerializerOptions specificConfig) {
                return new NQuadsSerializer(model, specificConfig);
            }
            throw new SerializationException(
                    "Invalid configuration type. Expected NQuadsSerializerOptions, got: " +
                            genericConfig.getClass().getSimpleName(),
                    RDFFormat.NQUADS.getName()
            );
        });
        tempDefaultRegistry.put(RDFFormat.NQUADS, NQuadsSerializer::new);

        tempRegistry.put(RDFFormat.TRIG, (model, genericConfig) -> {
            if (genericConfig instanceof TriGSerializerOptions specificConfig) {
                return new TriGSerializer(model, specificConfig);
            }
            throw new SerializationException(
                    "Invalid configuration type. Expected TriGSerializerOptions, got: " +
                            genericConfig.getClass().getSimpleName(),
                    RDFFormat.TRIG.getName()
            );
        });
        tempDefaultRegistry.put(RDFFormat.TRIG, TriGSerializer::new);

        tempRegistry.put(RDFFormat.RDFXML, (model, genericConfig) -> {
            if (genericConfig instanceof RDFXMLSerializerOptions specificConfig) {
                return new RDFXMLSerializer(model, specificConfig);
            }
            throw new SerializationException(
                    "Invalid configuration type. Expected RDFXMLSerializerOption, got: " +
                            genericConfig.getClass().getSimpleName(),
                    RDFFormat.RDFXML.getName()
            );
        });
        tempDefaultRegistry.put(RDFFormat.RDFXML, RDFXMLSerializer::new);

        tempRegistry.put(RDFFormat.JSONLD, (model, genericConfig) -> {
            if (genericConfig instanceof JSONLDOptions specificConfig) {
                return new JSONLDSerializer(model, specificConfig);
            }
            throw new SerializationException(
                    "Invalid configuration type. Expected JSONLDOptions, got: " +
                            genericConfig.getClass().getSimpleName(),
                    RDFFormat.JSONLD.getName()
            );
        });
        tempDefaultRegistry.put(RDFFormat.JSONLD, JSONLDSerializer::new);

        tempRegistry.put(RDFFormat.RDFC_1_0, (model, genericConfig) -> {
            if (genericConfig instanceof RDFC10SerializerOptions specificConfig) {
                RDFC10Canonicalizer rdfc10Canonicalizer = new RDFC10Canonicalizer(
                        specificConfig.getHashAlgorithm(),
                        specificConfig.getPermutationLimit(),
                        coreseValueFactory
                );
                return new RDFC10Serializer(model, specificConfig, rdfc10Canonicalizer);
            }
            throw new SerializationException(
                    "Invalid configuration type. Expected RDFC10SerializerOptions, got: " +
                            (genericConfig != null ? genericConfig.getClass().getSimpleName() : "null"),
                    RDFFormat.RDFC_1_0.getName()
            );
        });
        tempDefaultRegistry.put(RDFFormat.RDFC_1_0, model -> {
            RDFC10SerializerOptions defaultConfig = RDFC10SerializerOptions.defaultConfig();
            RDFC10Canonicalizer rdfc10Canonicalizer = new RDFC10Canonicalizer(
                    defaultConfig.getHashAlgorithm(),
                    defaultConfig.getPermutationLimit(),
                    coreseValueFactory
            );
            return new RDFC10Serializer(model, defaultConfig, rdfc10Canonicalizer);
        });

        this.registry = Collections.unmodifiableMap(tempRegistry);
        this.defaultRegistry = Collections.unmodifiableMap(tempDefaultRegistry);
    }

    /**
     * Creates an {@link RDFSerializer} instance for the specified format, model,
     * and configuration.
     *
     * @param format the {@link RDFFormat} for which to create the serializer. Must
     *               not be null.
     * @param model  the {@link Model} to be serialized. Must not be null.
     * @param config the {@link IOOptions} to apply during serialization.
     *               Must not be null.
     * @return a new instance of {@link RDFSerializer} configured for the specified
     *         format.
     * @throws NullPointerException     if any of the arguments (format, model,
     *                                  config) are null.
     * @throws SerializationException   if the provided format is not supported by
     *                                  this factory or if the configuration type is invalid.
     */
    @Override
    public RDFSerializer createSerializer(RDFFormat format, Model model, IOOptions config) {

        Objects.requireNonNull(format, "RDFFormat cannot be null");
        Objects.requireNonNull(model, "Model cannot be null");
        Objects.requireNonNull(config, "SerializationConfig cannot be null");

        BiFunction<Model, IOOptions, RDFSerializer> constructor = registry.get(format);

        if (constructor == null) {
            throw new SerializationException(
                    "Unsupported RDF format: " + format.getName(),
                    format.getName()
            );
        }

        return constructor.apply(model, config);
    }

    /**
     * Creates an {@link RDFSerializer} instance for the specified format and model
     * using the default configuration for that format.
     *
     * @param format the {@link RDFFormat} for which to create the serializer. Must
     *               not be null.
     * @param model  the {@link Model} to be serialized. Must not be null.
     * @return a new instance of {@link RDFSerializer} configured for the specified
     * format with default configuration.
     * @throws NullPointerException   if any of the arguments (format, model) are null.
     * @throws SerializationException if the provided format is not supported by
     *                                this factory.
     */
    @Override
    public RDFSerializer createSerializer(RDFFormat format, Model model) {

        Objects.requireNonNull(format, "RDFFormat cannot be null");
        Objects.requireNonNull(model, "Model cannot be null");

        Function<Model, RDFSerializer> constructor = defaultRegistry.get(format);

        if (constructor == null) {
            throw new SerializationException(
                    "Unsupported RDF format: " + format.getName(),
                    format.getName()
            );
        }

        return constructor.apply(model);
    }
}