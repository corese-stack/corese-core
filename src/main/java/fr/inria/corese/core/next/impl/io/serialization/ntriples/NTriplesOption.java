package fr.inria.corese.core.next.impl.io.serialization.ntriples;

import fr.inria.corese.core.next.impl.io.serialization.option.AbstractNFamilyOption;

/**
 * Configuration for N-Triples serialization format.
 * This class extends {@link AbstractNFamilyOption} and provides specific defaults
 * and options tailored for N-Triples, which is a simple, line-oriented format.
 *
 * <p>Use the {@link Builder} class to create instances of {@code NTriplesConfig}.
 * A predefined default configuration is available via {@link #defaultConfig()}.</p>
 */
public class NTriplesOption extends AbstractNFamilyOption {

    /**
     * Protected constructor to be used by the {@link Builder}.
     *
     * @param builder The builder instance containing the desired configuration values.
     */
    protected NTriplesOption(Builder builder) {
        super(builder);
    }

    /**
     * Public Builder for {@link NTriplesOption}.
     * Provides a fluent API for constructing {@code NTriplesConfig} instances with default values
     * specific to the N-Triples format.
     */
    public static class Builder extends AbstractNFamilyOption.AbstractNFamilyBuilder<Builder> {
        /**
         * Default constructor initializes all options with their default values for N-Triples.
         */
        public Builder() {

            includeContext(false);
        }

        /**
         * Builds and returns a new {@link NTriplesOption} instance with the current builder settings.
         *
         * @return A new {@code NTriplesConfig} instance.
         */
        @Override
        public NTriplesOption build() {
            return new NTriplesOption(this);
        }
    }

    /**
     * Returns a default configuration suitable for N-Triples serialization.
     * This provides a convenient way to get a standard N-Triples configuration without
     * manually building it.
     *
     * @return A {@code NTriplesConfig} instance with default settings.
     */
    public static NTriplesOption defaultConfig() {
        return new Builder().build();
    }

    /**
     * Returns a new builder instance for {@link NTriplesOption}.
     * This allows for fluent construction of custom N-Triples configurations.
     *
     * @return A new {@code Builder} instance.
     */
    public static NTriplesOption.Builder builder() {
        return new NTriplesOption.Builder();
    }
}
