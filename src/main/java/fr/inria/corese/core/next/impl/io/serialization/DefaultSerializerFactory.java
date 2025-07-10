package fr.inria.corese.core.next.impl.io.serialization;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.io.serialization.RDFSerializer;
import fr.inria.corese.core.next.api.io.serialization.SerializationConfig;
import fr.inria.corese.core.next.api.io.serialization.SerializerFactory;
import fr.inria.corese.core.next.impl.io.serialization.nquads.NQuadsOption;
import fr.inria.corese.core.next.impl.io.serialization.nquads.NQuadsSerializer;
import fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesOption;
import fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesSerializer;
import fr.inria.corese.core.next.impl.io.serialization.option.*;
import fr.inria.corese.core.next.impl.io.serialization.rdfxml.XmlOption;
import fr.inria.corese.core.next.impl.io.serialization.rdfxml.XmlSerializer;
import fr.inria.corese.core.next.impl.io.serialization.trig.TriGOption;
import fr.inria.corese.core.next.impl.io.serialization.trig.TriGSerializer;
import fr.inria.corese.core.next.impl.io.serialization.turtle.TurtleOption;
import fr.inria.corese.core.next.impl.io.serialization.turtle.TurtleSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Default implementation of {@link SerializerFactory}.
 * This factory is responsible for creating instances of {@link RDFSerializer}
 * based on the requested {@link RDFFormat}. It uses a registry pattern
 * to map each format to its corresponding serializer constructor,
 * providing a flexible and extensible way to manage serializer instances.
 *
 * <p>It adapts the generic {@link SerializationConfig} provided to the specific
 * configuration type expected by each serializer in the hierarchy, with a fallback
 * to default configurations if an incompatible type is provided.</p>
 */
public class DefaultSerializerFactory implements SerializerFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSerializerFactory.class);

    private final Map<RDFFormat, BiFunction<Model, SerializationConfig, RDFSerializer>> registry;

    /**
     * Constructs a {@code DefaultSerializerFactory} and populates its registry
     * with constructors for all known {@link RDFFormat} implementations.
     * Each constructor attempts to cast the generic {@link SerializationConfig} to the
     * specific configuration type required by the serializer. If the cast is not possible,
     * it falls back to the format's default configuration.
     */
    public DefaultSerializerFactory() {
        Map<RDFFormat, BiFunction<Model, SerializationConfig, RDFSerializer>> tempRegistry = new HashMap<>();

        tempRegistry.put(RDFFormat.TURTLE, (model, genericConfig) -> {
            if (genericConfig instanceof TurtleOption specificConfig) {
                return new TurtleSerializer(model, specificConfig);
            } else {
                logger.warn("Provided config for TURTLE is not TurtleConfig (was {}). Using default TurtleConfig.",
                        genericConfig.getClass().getSimpleName());
                return new TurtleSerializer(model, TurtleOption.defaultConfig());
            }
        });

        tempRegistry.put(RDFFormat.NTRIPLES, (model, genericConfig) -> {
            if (genericConfig instanceof NTriplesOption specificConfig) {
                return new NTriplesSerializer(model, specificConfig);
            } else {
                logger.warn("Provided config for NTRIPLES is not NTriplesConfig (was {}). Using default NTriplesConfig.",
                        genericConfig.getClass().getSimpleName());
                return new NTriplesSerializer(model, NTriplesOption.defaultConfig());
            }
        });

        tempRegistry.put(RDFFormat.NQUADS, (model, genericConfig) -> {
            if (genericConfig instanceof NQuadsOption specificConfig) {
                return new NQuadsSerializer(model, specificConfig);
            } else {
                logger.warn("Provided config for NQUADS is not NQuadsConfig (was {}). Using default NQuadsConfig.",
                        genericConfig.getClass().getSimpleName());
                return new NQuadsSerializer(model, NQuadsOption.defaultConfig());
            }
        });

        tempRegistry.put(RDFFormat.TRIG, (model, genericConfig) -> {
            if (genericConfig instanceof TriGOption specificConfig) {
                return new TriGSerializer(model, specificConfig);
            } else {
                logger.warn("Provided config for TRIG is not TriGConfig (was {}). Using default TriGConfig.",
                        genericConfig.getClass().getSimpleName());
                return new TriGSerializer(model, TriGOption.defaultConfig());
            }
        });

        tempRegistry.put(RDFFormat.RDFXML, (model, genericConfig) -> {
            if (genericConfig instanceof XmlOption specificConfig) {
                return new XmlSerializer(model, specificConfig);
            } else {
                logger.warn("Provided config for RDFXML is not RDFXmlConfig (was {}). Using default RDFXmlConfig.",
                        genericConfig.getClass().getSimpleName());
                return new XmlSerializer(model, XmlOption.defaultConfig());
            }
        });

        this.registry = Collections.unmodifiableMap(tempRegistry);
    }

    /**
     * Creates an {@link RDFSerializer} instance for the specified format, model, and configuration.
     *
     * @param format the {@link RDFFormat} for which to create the serializer. Must not be null.
     * @param model  the {@link Model} to be serialized. Must not be null.
     * @param config the {@link SerializationConfig} to apply during serialization. Must not be null.
     * @return a new instance of {@link RDFSerializer} configured for the specified format.
     * @throws NullPointerException     if any of the arguments (format, model, config) are null.
     * @throws IllegalArgumentException if the provided format is not supported by this factory.
     */
    @Override
    public RDFSerializer createSerializer(RDFFormat format, Model model, SerializationConfig config) {

        Objects.requireNonNull(format, "RDFFormat cannot be null");
        Objects.requireNonNull(model, "Model cannot be null");
        Objects.requireNonNull(config, "SerializationConfig cannot be null");

        BiFunction<Model, SerializationConfig, RDFSerializer> constructor = registry.get(format);

        if (constructor == null) {
            throw new IllegalArgumentException("Unsupported RDFFormat: " + format.getName());
        }

        return constructor.apply(model, config);
    }
}
