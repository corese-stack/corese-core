package fr.inria.corese.core.next.impl.io.serialization.option;

/**
 * Configuration for Canonical RDF serialization format (RDFC-1.0).
 * This class extends {@link AbstractSerializerOption} and provides specific defaults
 * and options tailored for the RDFC-1.0 canonicalization algorithm.
 * It includes options relevant to blank node canonicalization, such as the hashing algorithm
 * to use, the depth factor for graph isomorphism, and the permutation limit.
 * Use the {@link Builder} class to create instances of {@code CanonicalOption}.
 * A predefined default configuration is available via {@link #defaultConfig()}.
 */
public class CanonicalOption extends AbstractSerializerOption {

    public enum HashAlgorithm {
        SHA_256,
        SHA_384
    }

    private final HashAlgorithm hashAlgorithm;
    private final int depthFactor;
    private final int permutationLimit;

    /**
     * Protected constructor to be used by the {@link Builder}.
     *
     * @param builder The builder instance containing the desired configuration values.
     */
    protected CanonicalOption(Builder builder) {
        super(builder);
        this.hashAlgorithm = builder.hashAlgorithm;
        this.depthFactor = builder.depthFactor;
        this.permutationLimit = builder.permutationLimit;
    }


    public HashAlgorithm getHashAlgorithm() {
        return hashAlgorithm;
    }


    public int getPermutationLimit() {
        return permutationLimit;
    }

    /**
     * Public Builder for {@link CanonicalOption}.
     * Provides a fluent API for constructing {@code CanonicalOption} instances with default values
     * specific to the Canonical RDF format.
     */
    public static class Builder extends AbstractSerializerOption.AbstractBuilder<Builder> {
        private HashAlgorithm hashAlgorithm = HashAlgorithm.SHA_256;
        private int depthFactor = 5;
        private int permutationLimit = 50000;

        public Builder() {
            //Default constructor initializes all options with their default values for Canonical RDF.
        }

        @Override
        public CanonicalOption build() {
            return new CanonicalOption(this);
        }
    }


    public static CanonicalOption defaultConfig() {
        return new Builder().build();
    }

    public static CanonicalOption.Builder builder() {
        return new CanonicalOption.Builder();
    }
}