package fr.inria.corese.core.next.impl.common.serialization.config;

import fr.inria.corese.core.next.impl.common.serialization.util.SerializationConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Turtle serialization format.
 * This class extends {@link AbstractTFamilyConfig} and provides specific defaults
 * and options tailored for Turtle, such as using collections and anonymous blank nodes.
 *
 * <p>Use the {@link Builder} class to create instances of {@code TurtleConfig}.
 * A predefined default configuration is available via {@link #defaultConfig()}.</p>
 */
public class TurtleConfig extends AbstractTFamilyConfig {

    /**
     * Protected constructor to be used by the {@link Builder}.
     *
     * @param builder The builder instance containing the desired configuration values.
     */
    protected TurtleConfig(Builder builder) {
        super(builder);
    }

    /**
     * Public Builder for {@link TurtleConfig}.
     * Provides a fluent API for constructing {@code TurtleConfig} instances with default values
     * specific to the Turtle format.
     */
    public static class Builder extends AbstractTFamilyConfig.AbstractTFamilyBuilder<Builder> {
        /**
         * Default constructor initializes all options with their default values for Turtle.
         */
        public Builder() {
            lineEnding(System.lineSeparator());
            validateURIs(false);
            useCollections(true);
            blankNodeStyle(BlankNodeStyleEnum.ANONYMOUS);

            Map<String, String> commonTurtlePrefixes = new HashMap<>();
            commonTurtlePrefixes.put("rdf", SerializationConstants.RDF_NS);
            commonTurtlePrefixes.put("rdfs", SerializationConstants.RDFS_NS);
            commonTurtlePrefixes.put("xsd", SerializationConstants.XSD_NS);
            commonTurtlePrefixes.put("owl", SerializationConstants.OWL_NS);
            addCustomPrefixes(commonTurtlePrefixes);


        }

        /**
         * Builds and returns a new {@link TurtleConfig} instance with the current builder settings.
         *
         * @return A new {@code TurtleConfig} instance.
         */
        @Override
        public TurtleConfig build() {
            return new TurtleConfig(this);
        }
    }

    /**
     * Returns a default configuration suitable for Turtle serialization.
     * This provides a convenient way to get a standard Turtle configuration without
     * manually building it.
     *
     * @return A {@code TurtleConfig} instance with default settings.
     */
    public static TurtleConfig defaultConfig() {
        return new Builder().build();
    }


    /**
     * Returns a new builder instance for {@link TurtleConfig}.
     * This allows for fluent construction of custom Turtle configurations.
     *
     * @return A new {@code Builder} instance.
     */
    public static TurtleConfig.Builder builder() {
        return new TurtleConfig.Builder();
    }
}
