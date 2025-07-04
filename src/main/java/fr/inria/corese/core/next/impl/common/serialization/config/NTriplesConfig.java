package fr.inria.corese.core.next.impl.common.serialization.config;

/**
 * Configuration for N-Triples serialization format.
 * This class extends {@link AbstractNFamilyConfig} and provides specific defaults
 * and options tailored for N-Triples, which is a simple, line-oriented format.
 *
 * <p>Use the {@link Builder} class to create instances of {@code NTriplesConfig}.
 * A predefined default configuration is available via {@link #defaultConfig()}.</p>
 */
public class NTriplesConfig extends AbstractNFamilyConfig {

    /**
     * Protected constructor to be used by the {@link Builder}.
     *
     * @param builder The builder instance containing the desired configuration values.
     */
    protected NTriplesConfig(Builder builder) {
        super(builder);
    }

    /**
     * Public Builder for {@link NTriplesConfig}.
     * Provides a fluent API for constructing {@code NTriplesConfig} instances with default values
     * specific to the N-Triples format.
     */
    public static class Builder extends AbstractNFamilyConfig.AbstractNFamilyBuilder<Builder> {
        /**
         * Default constructor initializes all options with their default values for N-Triples.
         */
        public Builder() {

            includeContext(false);
        }

        /**
         * Builds and returns a new {@link NTriplesConfig} instance with the current builder settings.
         *
         * @return A new {@code NTriplesConfig} instance.
         */
        @Override
        public NTriplesConfig build() {
            return new NTriplesConfig(this);
        }
    }

    /**
     * Returns a default configuration suitable for N-Triples serialization.
     * This provides a convenient way to get a standard N-Triples configuration without
     * manually building it.
     *
     * @return A {@code NTriplesConfig} instance with default settings.
     */
    public static NTriplesConfig defaultConfig() {
        return new Builder().build();
    }

    /**
     * Returns a new builder instance for {@link NTriplesConfig}.
     * This allows for fluent construction of custom N-Triples configurations.
     *
     * @return A new {@code Builder} instance.
     */
    public static NTriplesConfig.Builder builder() {
        return new NTriplesConfig.Builder();
    }
}
