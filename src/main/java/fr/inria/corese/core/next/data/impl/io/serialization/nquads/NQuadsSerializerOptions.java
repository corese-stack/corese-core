package fr.inria.corese.core.next.data.impl.io.serialization.nquads;

import fr.inria.corese.core.next.data.impl.io.serialization.option.AbstractNFamilyOptions;

/**
 * Configuration for N-Quads serialization format.
 * This class extends {@link AbstractNFamilyOptions} and provides specific defaults
 * and options tailored for N-Quads, which extends N-Quads with named graphs.
 *
 * <p>Use the {@link Builder} class to create instances of {@code NQuadsConfig}.
 * A predefined default configuration is available via {@link #defaultConfig()}.</p>
 */
public class NQuadsSerializerOptions extends AbstractNFamilyOptions {

    /**
     * Protected constructor to be used by the {@link Builder}.
     *
     * @param builder The builder instance containing the desired configuration values.
     */
    protected NQuadsSerializerOptions(Builder builder) {
        super(builder);
    }

    /**
     * Public Builder for {@link NQuadsSerializerOptions}.
     * Provides a fluent API for constructing {@code NQuadsConfig} instances with default values
     * specific to the N-Quads format.
     */
    public static class Builder extends AbstractNFamilyOptions.AbstractNFamilyBuilder<Builder> {
        /**
         * Default constructor initializes all options with their default values for N-Quads.
         */
        public Builder() {
            includeContext(true);
        }

        /**
         * Builds and returns a new {@link NQuadsSerializerOptions} instance with the current builder settings.
         *
         * @return A new {@code NQuadsConfig} instance.
         */
        @Override
        public NQuadsSerializerOptions build() {
            return new NQuadsSerializerOptions(this);
        }
    }

    /**
     * Returns a default configuration suitable for N-Quads serialization.
     * This provides a convenient way to get a standard N-Quads configuration without
     * manually building it.
     *
     * @return A {@code NQuadsConfig} instance with default settings.
     */
    public static NQuadsSerializerOptions defaultConfig() {
        return new Builder().build();
    }

    /**
     * Returns a new builder instance for {@link NQuadsSerializerOptions}.
     * This allows for fluent construction of custom N-Quads configurations.
     *
     * @return A new {@code Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }
}
