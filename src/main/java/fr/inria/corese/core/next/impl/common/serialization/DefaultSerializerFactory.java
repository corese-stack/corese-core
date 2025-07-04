package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.RdfSerializer;
import fr.inria.corese.core.next.api.SerializationConfig;
import fr.inria.corese.core.next.api.SerializerFactory;
import fr.inria.corese.core.next.impl.common.serialization.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Default implementation of {@link SerializerFactory}.
 * This factory is responsible for creating instances of {@link RdfSerializer}
 * based on the requested {@link RdfFormat}. It uses a registry pattern
 * to map each format to its corresponding serializer constructor,
 * providing a flexible and extensible way to manage serializer instances.
 *
 * <p>It adapts the generic {@link SerializationConfig} provided to the specific
 * configuration type expected by each serializer in the hierarchy, with a fallback
 * to default configurations if an incompatible type is provided.</p>
 */
public class DefaultSerializerFactory implements SerializerFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSerializerFactory.class);

    private final Map<RdfFormat, BiFunction<Model, SerializationConfig, RdfSerializer>> registry;

    /**
     * Constructs a {@code DefaultSerializerFactory} and populates its registry
     * with constructors for all known {@link RdfFormat} implementations.
     * Each constructor attempts to cast the generic {@link SerializationConfig} to the
     * specific configuration type required by the serializer. If the cast is not possible,
     * it falls back to the format's default configuration.
     */
    public DefaultSerializerFactory() {
        Map<RdfFormat, BiFunction<Model, SerializationConfig, RdfSerializer>> tempRegistry = new HashMap<>();

        tempRegistry.put(RdfFormat.TURTLE, (model, genericConfig) -> {
            if (genericConfig instanceof TurtleConfig specificConfig) {
                return new TurtleSerializer(model, specificConfig);
            } else {
                logger.warn("Provided config for TURTLE is not TurtleConfig (was {}). Using default TurtleConfig.",
                        genericConfig.getClass().getSimpleName());
                return new TurtleSerializer(model, TurtleConfig.defaultConfig());
            }
        });

        tempRegistry.put(RdfFormat.NTRIPLES, (model, genericConfig) -> {
            if (genericConfig instanceof NTriplesConfig specificConfig) {
                return new NTriplesSerializer(model, specificConfig);
            } else {
                logger.warn("Provided config for NTRIPLES is not NTriplesConfig (was {}). Using default NTriplesConfig.",
                        genericConfig.getClass().getSimpleName());
                return new NTriplesSerializer(model, NTriplesConfig.defaultConfig());
            }
        });

        tempRegistry.put(RdfFormat.NQUADS, (model, genericConfig) -> {
            if (genericConfig instanceof NQuadsConfig specificConfig) {
                return new NQuadsSerializer(model, specificConfig);
            } else {
                logger.warn("Provided config for NQUADS is not NQuadsConfig (was {}). Using default NQuadsConfig.",
                        genericConfig.getClass().getSimpleName());
                return new NQuadsSerializer(model, NQuadsConfig.defaultConfig());
            }
        });

        tempRegistry.put(RdfFormat.TRIG, (model, genericConfig) -> {
            if (genericConfig instanceof TriGConfig specificConfig) {
                return new TriGSerializer(model, specificConfig);
            } else {
                logger.warn("Provided config for TRIG is not TriGConfig (was {}). Using default TriGConfig.",
                        genericConfig.getClass().getSimpleName());
                return new TriGSerializer(model, TriGConfig.defaultConfig());
            }
        });

        tempRegistry.put(RdfFormat.RDFXML, (model, genericConfig) -> {
            if (genericConfig instanceof XmlConfig specificConfig) {
                return new XmlSerializer(model, specificConfig);
            } else {
                logger.warn("Provided config for RDFXML is not RdfXmlConfig (was {}). Using default RdfXmlConfig.",
                        genericConfig.getClass().getSimpleName());
                return new XmlSerializer(model, XmlConfig.defaultConfig());
            }
        });

        this.registry = Collections.unmodifiableMap(tempRegistry);
    }

    /**
     * Creates an {@link RdfSerializer} instance for the specified format, model, and configuration.
     *
     * @param format the {@link RdfFormat} for which to create the serializer. Must not be null.
     * @param model  the {@link Model} to be serialized. Must not be null.
     * @param config the {@link SerializationConfig} to apply during serialization. Must not be null.
     * @return a new instance of {@link RdfSerializer} configured for the specified format.
     * @throws NullPointerException     if any of the arguments (format, model, config) are null.
     * @throws IllegalArgumentException if the provided format is not supported by this factory.
     */
    @Override
    public RdfSerializer createSerializer(RdfFormat format, Model model, SerializationConfig config) {

        Objects.requireNonNull(format, "RdfFormat cannot be null");
        Objects.requireNonNull(model, "Model cannot be null");
        Objects.requireNonNull(config, "SerializationConfig cannot be null");

        BiFunction<Model, SerializationConfig, RdfSerializer> constructor = registry.get(format);

        if (constructor == null) {
            throw new IllegalArgumentException("Unsupported RdfFormat: " + format.getName());
        }

        return constructor.apply(model, config);
    }
}
