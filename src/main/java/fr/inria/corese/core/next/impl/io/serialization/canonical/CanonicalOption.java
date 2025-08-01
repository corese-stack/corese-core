package fr.inria.corese.core.next.impl.io.serialization.canonical;

import fr.inria.corese.core.next.impl.io.serialization.option.AbstractSerializerOption;

/**
 * Configuration for Canonical RDF serialization format.
 * This class extends {@link AbstractSerializerOption} and provides specific defaults
 * and options tailored for canonicalization.
 *
 * <p>It includes options relevant to blank node canonicalization, such as whether to
 * include comments (which might interfere with strict canonicalization) or
 * to ensure deterministic blank node labeling.</p>
 *
 * <p>Use the {@link Builder} class to create instances of {@code CanonicalOption}.
 * A predefined default configuration is available via {@link #defaultConfig()}.</p>
 */
public class CanonicalOption extends AbstractSerializerOption {

    /**
     * Protected constructor to be used by the {@link Builder}.
     *
     * @param builder The builder instance containing the desired configuration values.
     */
    protected CanonicalOption(Builder builder) {
        super(builder);

    }

    /**
     * Public Builder for {@link CanonicalOption}.
     * Provides a fluent API for constructing {@code CanonicalOption} instances with default values
     * specific to the Canonical RDF format.
     */
    public static class Builder extends AbstractSerializerOption.AbstractBuilder<Builder> {
        /**
         * Default constructor initializes all options with their default values for Canonical RDF.
         */
        public Builder() {
            strictMode(true);
            validateURIs(true);
            escapeUnicode(true);
            trailingDot(true);
            includeContext(false);
        }

        /**
         * Builds and returns a new {@link CanonicalOption} instance with the current builder settings.
         *
         * @return A new {@code CanonicalOption} instance.
         */
        @Override
        public CanonicalOption build() {
            return new CanonicalOption(this);
        }
    }

    /**
     * Returns a default configuration suitable for Canonical RDF serialization.
     * This provides a convenient way to get a standard Canonical RDF configuration without
     * manually building it.
     *
     * @return A {@code CanonicalOption} instance with default settings.
     */
    public static CanonicalOption defaultConfig() {
        return new Builder().build();
    }

    /**
     * Returns a new builder instance for {@link CanonicalOption}.
     * This allows for fluent construction of custom Canonical RDF configurations.
     *
     * @return A new {@code Builder} instance.
     */
    public static CanonicalOption.Builder builder() {
        return new CanonicalOption.Builder();
    }
}
