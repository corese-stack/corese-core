package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.IRdfSerializer;
import fr.inria.corese.core.next.api.ISerializerFactory;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ISerializationConfig;
import fr.inria.corese.core.next.impl.common.serialization.config.FormatConfig;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Default implementation of {@link ISerializerFactory}.
 * This factory is responsible for creating instances of {@link IRdfSerializer}
 * based on the requested {@link RdfFormat}. It uses a registry pattern
 * to map each format to its corresponding serializer constructor,
 * providing a flexible and extensible way to manage serializer instances.
 */
public class DefaultSerializerFactory implements ISerializerFactory {

    private final Map<RdfFormat, BiFunction<Model, ISerializationConfig, IRdfSerializer>> registry;

    /**
     * Constructs a {@code DefaultSerializerFactory} and populates its registry
     * with constructors for all known {@link RdfFormat} implementations.
     */
    public DefaultSerializerFactory() {
        Map<RdfFormat, BiFunction<Model, ISerializationConfig, IRdfSerializer>> tempRegistry = new HashMap<>();
        // Cast the lambda to BiFunction<Model, IFormatConfig, IRdfSerializer>
        tempRegistry.put(RdfFormat.TURTLE, (model, config) -> new TurtleSerializer(model, (FormatConfig) config));
        tempRegistry.put(RdfFormat.NTRIPLES, (model, config) -> new NTriplesSerializer(model, (FormatConfig) config));
        tempRegistry.put(RdfFormat.NQUADS, (model, config) -> new NQuadsSerializer(model, (FormatConfig) config));
        tempRegistry.put(RdfFormat.TRIG, (model, config) -> new TriGSerializer(model, (FormatConfig) config));
        tempRegistry.put(RdfFormat.RDFXML, (model, config) -> new XmlSerializer(model, (FormatConfig) config));

        this.registry = Collections.unmodifiableMap(tempRegistry);
    }

    /**
     * Creates an {@link IRdfSerializer} instance for the specified format, model, and configuration.
     *
     * @param format the {@link RdfFormat} for which to create the serializer. Must not be null.
     * @param model the {@link Model} to be serialized. Must not be null.
     * @param config the {@link ISerializationConfig} to apply during serialization. Must not be null.
     * @return a new instance of {@link IRdfSerializer} configured for the specified format.
     * @throws NullPointerException if any of the arguments (format, model, config) are null.
     * @throws IllegalArgumentException if the provided format is not supported by this factory.
     */
    @Override
    public IRdfSerializer createSerializer(RdfFormat format, Model model, ISerializationConfig config) {

        Objects.requireNonNull(format, "RdfFormat cannot be null");
        Objects.requireNonNull(model, "Model cannot be null");
        Objects.requireNonNull(config, "IFormatConfig cannot be null");

        BiFunction<Model, ISerializationConfig, IRdfSerializer> constructor = registry.get(format);

        if (constructor == null) {
            throw new IllegalArgumentException("Unsupported RdfFormat: " + format.getName());
        }


        return constructor.apply(model, config);
    }
}
