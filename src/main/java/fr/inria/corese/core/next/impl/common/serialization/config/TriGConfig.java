package fr.inria.corese.core.next.impl.common.serialization.config;

import fr.inria.corese.core.next.impl.common.serialization.util.SerializationConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for TriG serialization format.
 * This class extends {@link AbstractTFamilyConfig} and provides specific defaults
 * and options tailored for TriG, which extends Turtle with named graphs.
 *
 * <p>Use the {@link Builder} class to create instances of {@code TriGConfig}.
 * A predefined default configuration is available via {@link #defaultConfig()}.</p>
 */
public class TriGConfig extends AbstractTFamilyConfig {

    /**
     * Protected constructor to be used by the {@link Builder}.
     *
     * @param builder The builder instance containing the desired configuration values.
     */
    protected TriGConfig(Builder builder) {
        super(builder);
    }

    /**
     * Public Builder for {@link TriGConfig}.
     * Provides a fluent API for constructing {@code TriGConfig} instances with default values
     * specific to the TriG format.
     */
    public static class Builder extends AbstractTFamilyConfig.AbstractTFamilyBuilder<Builder> {
        /**
         * Default constructor initializes all options with their default values for TriG.
         */
        public Builder() {
            includeContext(true);
            blankNodeStyle(BlankNodeStyleEnum.NAMED);
            useCollections(false);

            Map<String, String> commonTriGPrefixes = new HashMap<>();
            commonTriGPrefixes.put("rdf", SerializationConstants.RDF_NS);
            commonTriGPrefixes.put("rdfs", SerializationConstants.RDFS_NS);
            commonTriGPrefixes.put("xsd", SerializationConstants.XSD_NS);
            commonTriGPrefixes.put("owl", SerializationConstants.OWL_NS);
            addCustomPrefixes(commonTriGPrefixes);

         }

        /**
         * Builds and returns a new {@link TriGConfig} instance with the current builder settings.
         *
         * @return A new {@code TriGConfig} instance.
         */
        @Override
        public TriGConfig build() {
            return new TriGConfig(this);
        }
    }

    /**
     * Returns a default configuration suitable for TriG serialization.
     * This provides a convenient way to get a standard TriG configuration without
     * manually building it.
     *
     * @return A {@code TriGConfig} instance with default settings.
     */
    public static TriGConfig defaultConfig() {
        return new Builder().build();
    }

    /**
     * Returns a new builder instance for {@link TriGConfig}.
     * This allows for fluent construction of custom TriG configurations.
     *
     * @return A new {@code Builder} instance.
     */
    public static TriGConfig.Builder builder() {
        return new TriGConfig.Builder();
    }
}
