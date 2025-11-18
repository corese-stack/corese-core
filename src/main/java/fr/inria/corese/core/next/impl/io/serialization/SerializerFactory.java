package fr.inria.corese.core.next.impl.io.serialization;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.io.serialization.RDFSerializer;
import fr.inria.corese.core.next.impl.io.serialization.canonical.RDFC10Canonicalizer;
import fr.inria.corese.core.next.impl.io.serialization.canonical.RDFC10Options;
import fr.inria.corese.core.next.impl.io.serialization.canonical.RDFC10Serializer;
import fr.inria.corese.core.next.impl.io.serialization.nquads.NQuadsSerializerOptions;
import fr.inria.corese.core.next.impl.io.serialization.nquads.NQuadsSerializer;
import fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesSerializerOptions;
import fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesSerializer;
import fr.inria.corese.core.next.impl.io.serialization.rdfxml.RDFXMLSerializerOption;
import fr.inria.corese.core.next.impl.io.serialization.rdfxml.RDFXMLSerializer;
import fr.inria.corese.core.next.impl.io.serialization.trig.TriGSerializerOptions;
import fr.inria.corese.core.next.impl.io.serialization.trig.TriGSerializer;
import fr.inria.corese.core.next.impl.io.serialization.turtle.TurtleSerializerOptions;
import fr.inria.corese.core.next.impl.io.serialization.turtle.TurtleSerializer;
import fr.inria.corese.core.next.impl.io.serialization.jsonld.JSONLDSerializer;
import fr.inria.corese.core.next.impl.io.option.JSONLDProcessorOptions;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Default implementation of {@link fr.inria.corese.core.next.api.io.serialization.SerializerFactory}.
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
public class SerializerFactory implements fr.inria.corese.core.next.api.io.serialization.SerializerFactory {

    private static final Logger logger = LoggerFactory.getLogger(SerializerFactory.class);

    private final Map<RDFFormat, BiFunction<Model, IOOptions, RDFSerializer>> registry;
    private final ValueFactory coreseValueFactory;

    /**
     * Constructs a {@code DefaultSerializerFactory} and populates its registry
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

        tempRegistry.put(RDFFormat.TURTLE, (model, genericConfig) -> {
            if (genericConfig instanceof TurtleSerializerOptions specificConfig) {
                return new TurtleSerializer(model, specificConfig);
            } else {
                logger.warn("Provided config for TURTLE is not TurtleConfig (was {}). Using default TurtleConfig.",
                        genericConfig.getClass().getSimpleName());
                return new TurtleSerializer(model, TurtleSerializerOptions.defaultConfig());
            }
        });

        tempRegistry.put(RDFFormat.NTRIPLES, (model, genericConfig) -> {
            if (genericConfig instanceof NTriplesSerializerOptions specificConfig) {
                return new NTriplesSerializer(model, specificConfig);
            } else {
                logger.warn(
                        "Provided config for NTRIPLES is not NTriplesConfig (was {}). Using default NTriplesConfig.",
                        genericConfig.getClass().getSimpleName());
                return new NTriplesSerializer(model, NTriplesSerializerOptions.defaultConfig());
            }
        });

        tempRegistry.put(RDFFormat.NQUADS, (model, genericConfig) -> {
            if (genericConfig instanceof NQuadsSerializerOptions specificConfig) {
                return new NQuadsSerializer(model, specificConfig);
            } else {
                logger.warn("Provided config for NQUADS is not NQuadsConfig (was {}). Using default NQuadsConfig.",
                        genericConfig.getClass().getSimpleName());
                return new NQuadsSerializer(model, NQuadsSerializerOptions.defaultConfig());
            }
        });

        tempRegistry.put(RDFFormat.TRIG, (model, genericConfig) -> {
            if (genericConfig instanceof TriGSerializerOptions specificConfig) {
                return new TriGSerializer(model, specificConfig);
            } else {
                logger.warn("Provided config for TRIG is not TriGConfig (was {}). Using default TriGConfig.",
                        genericConfig.getClass().getSimpleName());
                return new TriGSerializer(model, TriGSerializerOptions.defaultConfig());
            }
        });

        tempRegistry.put(RDFFormat.RDFXML, (model, genericConfig) -> {
            if (genericConfig instanceof RDFXMLSerializerOption specificConfig) {
                return new RDFXMLSerializer(model, specificConfig);
            } else {
                logger.warn("Provided config for RDFXML is not RDFXmlConfig (was {}). Using default RDFXmlConfig.",
                        genericConfig.getClass().getSimpleName());
                return new RDFXMLSerializer(model, RDFXMLSerializerOption.defaultConfig());
            }
        });

        tempRegistry.put(RDFFormat.JSONLD, (model, genericConfig) -> {
            if (genericConfig instanceof JSONLDProcessorOptions specificConfig) {
                return new JSONLDSerializer(model, specificConfig);
            } else {
                logger.warn(
                        "Provided config for JSONLD is not TitaniumJSONLDProcessorOption (was {}). Using default TitaniumJSONLDProcessorOption.",
                        genericConfig.getClass().getSimpleName());
                return new JSONLDSerializer(model, new JSONLDProcessorOptions.Builder().build());
            }
        });

        tempRegistry.put(RDFFormat.RDFC_1_0, (model, genericConfig) -> {
            if (genericConfig instanceof RDFC10Options specificConfig) {
                RDFC10Canonicalizer canonicalizer = new RDFC10Canonicalizer(
                        specificConfig.getHashAlgorithm(),
                        specificConfig.getPermutationLimit(),
                        coreseValueFactory
                );
                return new RDFC10Serializer(model, specificConfig, canonicalizer);
            } else {
                logger.warn("Provided config for RDFC_1_0 is not CanonicalOption (was {}). Using default CanonicalOption.",
                        genericConfig != null ? genericConfig.getClass().getSimpleName() : "null");
                RDFC10Options defaultConfig = RDFC10Options.defaultConfig();
                RDFC10Canonicalizer canonicalizer = new RDFC10Canonicalizer(
                        defaultConfig.getHashAlgorithm(),
                        defaultConfig.getPermutationLimit(),
                        coreseValueFactory
                );
                return new RDFC10Serializer(model, defaultConfig, canonicalizer);
            }
        });


        tempRegistry.put(RDFFormat.JSONLD, (model, genericConfig) -> {
            if (genericConfig instanceof JSONLDProcessorOptions specificConfig) {
                return new JSONLDSerializer(model, specificConfig);
            } else {
                logger.warn(
                        "Provided config for JSONLD is not TitaniumJSONLDProcessorOption (was {}). Using default TitaniumJSONLDProcessorOption.",
                        genericConfig.getClass().getSimpleName());
                return new JSONLDSerializer(model, new JSONLDProcessorOptions.Builder().build());
            }
        });

        this.registry = Collections.unmodifiableMap(tempRegistry);
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
     * @throws IllegalArgumentException if the provided format is not supported by
     *                                  this factory.
     */
    @Override
    public RDFSerializer createSerializer(RDFFormat format, Model model, IOOptions config) {

        Objects.requireNonNull(format, "RDFFormat cannot be null");
        Objects.requireNonNull(model, "Model cannot be null");
        Objects.requireNonNull(config, "SerializationConfig cannot be null");

        BiFunction<Model, IOOptions, RDFSerializer> constructor = registry.get(format);

        if (constructor == null) {
            throw new IllegalArgumentException("Unsupported RDFFormat: " + format.getName());
        }

        return constructor.apply(model, config);
    }
}