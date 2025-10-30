package fr.inria.corese.core.next.impl.io.serialization.canonical;

import fr.inria.corese.core.next.impl.io.serialization.option.AbstractSerializerOption;

/**
 * Configuration for Canonical RDF serialization format (RDFC-1.0).
 * This class extends {@link AbstractSerializerOption} and provides specific defaults
 * and options tailored for the RDFC-10 canonicalization algorithm.
 * It includes options relevant to blank node canonicalization, such as the hashing algorithm
 * to use, the depth factor for graph isomorphism, and the permutation limit.
 * Use the {@link Builder} class to create instances of {@code CanonicalOption}.
 * A predefined default configuration is available via {@link #defaultConfig()}.
 */
public class RDFC10Options extends AbstractSerializerOption {

    /**
     * Enumeration for the supported hashing algorithms.
     */
    public enum HashAlgorithm {
        SHA_256,
        SHA_384
    }

    private final HashAlgorithm hashAlgorithm;
    private final int depthFactor;
    private final int permutationLimit;

    /**
     * Protected constructor to be used by the {@link Builder}.
     * It initializes a new instance of {@code CanonicalOption} with the values
     * provided by the builder.
     *
     * @param builder The builder instance containing the desired configuration values.
     */
    protected RDFC10Options(Builder builder) {
        super(builder);
        this.hashAlgorithm = builder.hashAlgorithm;
        this.depthFactor = builder.depthFactor;
        this.permutationLimit = builder.permutationLimit;
    }

    /**
     * Gets the hashing algorithm used for blank node canonicalization.
     *
     * @return The {@link HashAlgorithm} used.
     */
    public HashAlgorithm getHashAlgorithm() {
        return hashAlgorithm;
    }

    /**
     * Gets the depth factor for graph isomorphism resolution.
     * This value is used to limit the depth of the recursive hashing algorithm.
     *
     * @return The depth factor.
     */
    public int getDepthFactor() {
        return depthFactor;
    }

    /**
     * Gets the permutation limit used in the canonicalization algorithm.
     * This value is used to limit the number of permutations attempted during blank node canonicalization
     * to prevent excessive computation time.
     *
     * @return The permutation limit.
     */
    public int getPermutationLimit() {
        return permutationLimit;
    }

    /**
     * Public Builder for {@link RDFC10Options}.
     * Provides a fluent API for constructing {@code CanonicalOption} instances with default values
     * specific to the Canonical RDF format.
     */
    public static class Builder extends AbstractSerializerOption.AbstractBuilder<Builder> {
        private HashAlgorithm hashAlgorithm = HashAlgorithm.SHA_256;
        private int depthFactor = 5;
        private int permutationLimit = 50000;

        /**
         * Default constructor for the Builder.
         * Initializes the builder with the default values for Canonical RDF serialization.
         */
        public Builder() {
            // Default constructor initializes
        }

        /**
         * Builds a new {@link RDFC10Options} instance with the configured values.
         *
         * @return A new instance of {@code CanonicalOption}.
         */
        @Override
        public RDFC10Options build() {
            return new RDFC10Options(this);
        }
    }

    /**
     * Creates and returns a new {@code CanonicalOption} instance with the default configuration.
     *
     * @return A new {@code CanonicalOption} with default settings.
     */
    public static RDFC10Options defaultConfig() {
        return new Builder().build();
    }

    /**
     * Creates and returns a new {@link Builder} instance, which can be used to customize
     * the {@code CanonicalOption} before building.
     *
     * @return A new {@code Builder} instance.
     */
    public static RDFC10Options.Builder builder() {
        return new RDFC10Options.Builder();
    }
}
