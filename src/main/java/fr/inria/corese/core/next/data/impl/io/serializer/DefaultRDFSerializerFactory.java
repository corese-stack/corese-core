package fr.inria.corese.core.next.data.impl.io.serializer;

import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.api.io.option.BaseIRIOptions;
import fr.inria.corese.core.next.data.api.io.option.RDFSerializationOptions;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializerOptions;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializer;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializerFactory;
import fr.inria.corese.core.next.data.api.exception.SerializationException;
import fr.inria.corese.core.next.data.impl.io.serializer.rdfc10.RDFC10Canonicalizer;
import fr.inria.corese.core.next.data.impl.io.serializer.rdfc10.RDFC10Serializer;
import fr.inria.corese.core.next.data.impl.io.serializer.rdfc10.RDFC10SerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.jsonld.JSONLDSerializer;
import fr.inria.corese.core.next.data.api.io.JSONLDOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.nquads.NQuadsSerializer;
import fr.inria.corese.core.next.data.impl.io.serializer.nquads.NQuadsSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.ntriples.NTriplesSerializer;
import fr.inria.corese.core.next.data.impl.io.serializer.ntriples.NTriplesSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.rdfxml.RDFXMLSerializer;
import fr.inria.corese.core.next.data.impl.io.serializer.rdfxml.RDFXMLSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.trig.TriGSerializer;
import fr.inria.corese.core.next.data.impl.io.serializer.trig.TriGSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.turtle.TurtleSerializer;
import fr.inria.corese.core.next.data.impl.io.serializer.turtle.TurtleSerializerOptions;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.data.impl.model.LinkedHashModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Default implementation of {@link RDFSerializerFactory}.
 * This factory is responsible for creating instances of {@link RDFSerializer}
 * based on the requested {@link RDFFormat}. It uses a registry pattern
 * to map each format to its corresponding serializer constructor,
 * providing a flexible and extensible way to manage serializer instances.
 *
 * <p>Shared public options are adapted to each format's safe defaults.
 * Incompatible format-specific options are rejected rather than ignored.</p>
 */
public class DefaultRDFSerializerFactory implements RDFSerializerFactory {

    private static final String RDF_FORMAT_CANNOT_BE_NULL = "RDFFormat cannot be null";

    private final Map<RDFFormat, BiFunction<Model, IOOptions, RDFSerializer>> registry;
    private final Map<RDFFormat, Function<Model, RDFSerializer>> defaultRegistry;
    private final ValueFactory coreseValueFactory;

    /**
     * Constructs a {@code RDFSerializerFactory} and populates its registry
     * with constructors for all known {@link RDFFormat} implementations.
     * Shared options are normalized before these constructors are invoked.
     */
    public DefaultRDFSerializerFactory() {
        this.coreseValueFactory = new CoreseValueFactory();

        Map<RDFFormat, BiFunction<Model, IOOptions, RDFSerializer>> tempRegistry = new HashMap<>();
        Map<RDFFormat, Function<Model, RDFSerializer>> tempDefaultRegistry = new HashMap<>();

        tempRegistry.put(RDFFormat.TURTLE, TurtleSerializer::new);
        tempDefaultRegistry.put(RDFFormat.TURTLE, TurtleSerializer::new);

        tempRegistry.put(RDFFormat.NTRIPLES, NTriplesSerializer::new);
        tempDefaultRegistry.put(RDFFormat.NTRIPLES, NTriplesSerializer::new);

        tempRegistry.put(RDFFormat.NQUADS, NQuadsSerializer::new);
        tempDefaultRegistry.put(RDFFormat.NQUADS, NQuadsSerializer::new);

        tempRegistry.put(RDFFormat.TRIG, TriGSerializer::new);
        tempDefaultRegistry.put(RDFFormat.TRIG, TriGSerializer::new);

        tempRegistry.put(RDFFormat.RDFXML, RDFXMLSerializer::new);
        tempDefaultRegistry.put(RDFFormat.RDFXML, RDFXMLSerializer::new);

        tempRegistry.put(RDFFormat.JSONLD, JSONLDSerializer::new);
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
    public RDFSerializer createSerializer(
            RDFFormat format,
            Model model,
            RDFSerializationOptions config) {

        Objects.requireNonNull(format, RDF_FORMAT_CANNOT_BE_NULL);
        Objects.requireNonNull(model, "Model cannot be null");
        Objects.requireNonNull(config, "SerializationConfig cannot be null");

        BiFunction<Model, IOOptions, RDFSerializer> constructor = registry.get(format);

        if (constructor == null) {
            throw new SerializationException(
                    "Unsupported RDF format: " + format.getName(),
                    format.getName()
            );
        }

        return constructor.apply(model, normalizeOptions(format, config));
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

        Objects.requireNonNull(format, RDF_FORMAT_CANNOT_BE_NULL);
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

    @Override
    public RDFSerializer createSerializer(RDFFormat format, Iterable<Statement> statements) {
        Objects.requireNonNull(format, RDF_FORMAT_CANNOT_BE_NULL);
        Objects.requireNonNull(statements, "Statements cannot be null");
        if (RDFFormat.NTRIPLES.equals(format)) {
            return new NTriplesSerializer(statements);
        }
        if (RDFFormat.NQUADS.equals(format)) {
            return new NQuadsSerializer(statements);
        }
        return createSerializer(format, new LinkedHashModel(coreseValueFactory, statements));
    }

    @Override
    public RDFSerializer createSerializer(
            RDFFormat format,
            Iterable<Statement> statements,
            RDFSerializationOptions config) {
        Objects.requireNonNull(format, RDF_FORMAT_CANNOT_BE_NULL);
        Objects.requireNonNull(statements, "Statements cannot be null");
        Objects.requireNonNull(config, "SerializationConfig cannot be null");
        RDFSerializationOptions normalized = normalizeOptions(format, config);
        if (RDFFormat.NTRIPLES.equals(format)) {
            return new NTriplesSerializer(statements, normalized);
        }
        if (RDFFormat.NQUADS.equals(format)) {
            return new NQuadsSerializer(statements, normalized);
        }
        return createSerializer(format, new LinkedHashModel(coreseValueFactory, statements), normalized);
    }

    private static RDFSerializationOptions normalizeOptions(
            RDFFormat format,
            RDFSerializationOptions options) {
        if (options instanceof RDFSerializerOptions) {
            return normalizeGenericOptions(format, options);
        }
        if (!isCompatibleOptionType(format, options)) {
            throw invalidOptions(format, options);
        }
        return options;
    }

    private static RDFSerializationOptions normalizeGenericOptions(
            RDFFormat format,
            RDFSerializationOptions options) {
        if (RDFFormat.TURTLE.equals(format)) {
            return new TurtleSerializerOptions.Builder(options).build();
        }
        if (RDFFormat.TRIG.equals(format)) {
            return new TriGSerializerOptions.Builder(options).build();
        }
        if (RDFFormat.RDFXML.equals(format)) {
            return new RDFXMLSerializerOptions.Builder(options).build();
        }
        if (RDFFormat.NTRIPLES.equals(format) || RDFFormat.NQUADS.equals(format)) {
            return options;
        }
        if (RDFFormat.JSONLD.equals(format)) {
            JSONLDOptions.Builder builder = new JSONLDOptions.Builder();
            String baseIRI = ((BaseIRIOptions) options).getBaseIRI();
            return baseIRI == null ? builder.build() : builder.base(baseIRI).build();
        }
        throw invalidOptions(format, options);
    }

    private static boolean isCompatibleOptionType(RDFFormat format, RDFSerializationOptions options) {
        return (RDFFormat.TURTLE.equals(format) && options instanceof TurtleSerializerOptions)
                || (RDFFormat.TRIG.equals(format) && options instanceof TriGSerializerOptions)
                || (RDFFormat.NTRIPLES.equals(format) && options instanceof NTriplesSerializerOptions)
                || (RDFFormat.NQUADS.equals(format) && options instanceof NQuadsSerializerOptions)
                || (RDFFormat.RDFXML.equals(format) && options instanceof RDFXMLSerializerOptions)
                || (RDFFormat.JSONLD.equals(format) && options instanceof JSONLDOptions)
                || (RDFFormat.RDFC_1_0.equals(format) && options instanceof RDFC10SerializerOptions);
    }

    private static SerializationException invalidOptions(RDFFormat format, IOOptions options) {
        return new SerializationException(
                "Options of type " + options.getClass().getSimpleName()
                        + " are not valid for " + format.getName(),
                format);
    }
}
