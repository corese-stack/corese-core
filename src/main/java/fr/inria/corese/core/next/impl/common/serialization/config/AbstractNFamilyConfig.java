package fr.inria.corese.core.next.impl.common.serialization.config;

/**
 * An abstract base class for serialization configurations of N-Family RDF formats (e.g., N-Triples, N-Quads).
 * This class extends {@link AbstractSerializerConfig} and provides a common foundation
 * for formats that typically have simpler, line-based structures and specific default behaviors
 * regarding literal datatypes and character escaping.
 *
 * <p>It enforces the use of the Builder pattern for construction through its
 * nested {@link AbstractNFamilyBuilder}. Subclasses are expected to extend this
 * configuration and its builder to add format-specific options.</p>
 */
public abstract class AbstractNFamilyConfig extends AbstractSerializerConfig {

    /**
     * Protected constructor to be used by concrete builder implementations.
     * Initializes the N-Family serialization configuration options, calling the superclass
     * constructor for common options.
     *
     * @param builder The builder instance containing the desired configuration values.
     */
    protected AbstractNFamilyConfig(AbstractNFamilyBuilder<?> builder) {
        super(builder);
    }

    /**
     * An abstract base builder for {@link AbstractNFamilyConfig}.
     * This builder provides methods for setting N-Family serialization configuration options.
     * It extends {@link AbstractSerializerConfig.AbstractBuilder} and uses a recursive type
     * parameter (`S`) to allow concrete subclass builders to return their own specific type,
     * enabling fluent API chaining.
     *
     * <p>By default, it sets {@code literalDatatypePolicy} to {@link LiteralDatatypePolicyEnum#ALWAYS_TYPED}
     * and {@code escapeUnicode} to {@code true}, which are common characteristics of N-Family formats.</p>
     *
     * @param <S> The type of the concrete builder extending this abstract builder.
     */
    public abstract static class AbstractNFamilyBuilder<S extends AbstractNFamilyBuilder<S>>
            extends AbstractSerializerConfig.AbstractBuilder<S> {

        /**
         * Default constructor for the builder.
         * Initializes common N-Family specific defaults:
         * <ul>
         * <li>{@code literalDatatypePolicy} is set to {@link LiteralDatatypePolicyEnum#ALWAYS_TYPED}.</li>
         * <li>{@code escapeUnicode} is set to {@code true}.</li>
         * </ul>
         */
        protected AbstractNFamilyBuilder() {
            super.literalDatatypePolicy = LiteralDatatypePolicyEnum.ALWAYS_TYPED;
            super.escapeUnicode = true;
        }

        /**
         * Builds and returns a new {@link AbstractNFamilyConfig} instance with the current builder settings.
         * This method must be implemented by concrete builder subclasses to return their specific configuration type.
         *
         * @return A new {@code AbstractNFamilyConfig} instance or a subclass instance.
         */
        public abstract AbstractNFamilyConfig build();
    }
}
