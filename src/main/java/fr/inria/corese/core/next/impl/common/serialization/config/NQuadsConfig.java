package fr.inria.corese.core.next.impl.common.serialization.config;

/**
 * Configuration for N-Quads serialization format.
 * This class extends {@link AbstractNFamilyConfig} and provides specific defaults
 * and options tailored for N-Quads, which extends N-Triples with named graphs.
 *
 * <p>Use the {@link Builder} class to create instances of {@code NQuadsConfig}.
 * A predefined default configuration is available via {@link #defaultConfig()}.</p>
 */
public class NQuadsConfig extends AbstractNFamilyConfig {

    /**
     * Protected constructor to be used by the {@link Builder}.
     *
     * @param builder The builder instance containing the desired configuration values.
     */
    protected NQuadsConfig(Builder builder) {
        super(builder);
    }

    /**
     * Public Builder for {@link NQuadsConfig}.
     * Provides a fluent API for constructing {@code NQuadsConfig} instances with default values
     * specific to the N-Quads format.
     */
    public static class Builder extends AbstractNFamilyConfig.AbstractNFamilyBuilder<Builder> {
        /**
         * Default constructor initializes all options with their default values for N-Quads.
         */
        public Builder() {
            includeContext(true);
        }

        /**
         * Builds and returns a new {@link NQuadsConfig} instance with the current builder settings.
         *
         * @return A new {@code NQuadsConfig} instance.
         */
        @Override
        public NQuadsConfig build() {
            return new NQuadsConfig(this);
        }
    }

    /**
     * Returns a default configuration suitable for N-Quads serialization.
     * This provides a convenient way to get a standard N-Quads configuration without
     * manually building it.
     *
     * @return A {@code NQuadsConfig} instance with default settings.
     */
    public static NQuadsConfig defaultConfig() {
        return new Builder().build();
    }

    /**
     * Returns a new builder instance for {@link NQuadsConfig}.
     * This allows for fluent construction of custom N-Quads configurations.
     *
     * @return A new {@code Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }
}
