package fr.inria.corese.core.next.impl.io.serialization.option;

import fr.inria.corese.core.next.api.io.serialization.SerializationOption;
import fr.inria.corese.core.next.impl.io.serialization.util.SerializationConstants;

import java.util.Objects;

/**
 * An abstract base class for all RDF serialization configurations.
 * This class defines common configuration parameters that are applicable across various
 * RDF serialization formats (e.g., N-Triples, Turtle, RDF/XML).
 *
 * <p>It enforces the use of the Builder pattern for construction through its
 * nested {@link AbstractBuilder}. Subclasses are expected to extend this
 * configuration and its builder to add format-specific options.</p>
 */
public abstract class AbstractSerializerOption implements SerializationOption {

    /**
     * The policy for how literal datatypes are printed.
     * This determines whether datatypes are always explicit, minimal, or follow specific rules.
     */
    protected final LiteralDatatypePolicyEnum literalDatatypePolicy;
    /**
     * Whether non-ASCII characters should be escaped using Unicode escape sequences (e.g., `\ u00E9`).
     * This ensures compatibility with systems that might not handle UTF-8 correctly, but makes output less human-readable.
     */
    protected final boolean escapeUnicode;
    /**
     * Whether a dot `.` should be added at the end of each triple block or statement.
     * This is a syntax requirement for some RDF serialization formats (e.g., Turtle, N-Triples).
     */
    protected final boolean trailingDot;
    /**
     * The base IRI to be used for the serialization.
     * This allows relative IRIs to be resolved and can shorten the output by avoiding full IRIs.
     * Can be {@code null} if no base IRI is specified.
     */
    protected final String baseIRI;
    /**
     * Whether deterministic blank node IDs (e.g., `_:b0`, `_:b1`) should be generated.
     * This is crucial for reproducible outputs, especially in testing environments, as blank node IDs are typically random.
     */
    protected final boolean stableBlankNodeIds;
    /**
     * The string used for line endings (e.g., `"\n"` for Unix, `"\r\n"` for Windows).
     * This ensures that the generated file has correct line endings for the target operating system.
     */
    protected final String lineEnding;

    /**
     * General strictness setting for validation during serialization.
     * Enabling this can catch errors or non-standard RDF constructs but might reject valid, less common patterns.
     */
    protected final boolean strictMode;
    /**
     * Whether URIs should be validated for compliance with RDF/Turtle/N-Triples rules.
     * This ensures that generated URIs are valid and will be correctly parsed by other tools.
     */
    protected final boolean validateURIs;
    /**
     * Whether context information (named graphs) should be included in the serialization output.
     * This is essential for formats like N-Quads or TriG which support named graphs.
     */
    protected final boolean includeContext;

    /**
     * Protected constructor to be used by concrete builder implementations.
     * Initializes the common serialization configuration options.
     *
     * @param builder The builder instance containing the desired configuration values.
     * @throws NullPointerException if any required field from the builder is null.
     */
    protected AbstractSerializerOption(AbstractBuilder<?> builder) {
        this.literalDatatypePolicy = Objects.requireNonNull(builder.literalDatatypePolicy, "Literal datatype policy cannot be null");
        this.escapeUnicode = builder.escapeUnicode;
        this.trailingDot = builder.trailingDot;
        this.baseIRI = builder.baseIRI;
        this.stableBlankNodeIds = builder.stableBlankNodeIds;
        this.lineEnding = Objects.requireNonNull(builder.lineEnding, "Line ending cannot be null");

        this.strictMode = builder.strictMode;
        this.validateURIs = builder.validateURIs;
        this.includeContext = builder.includeContext;
    }

    // --- Getters for common properties ---

    /**
     * Returns the policy for how literal datatypes are printed.
     *
     * @return The {@link LiteralDatatypePolicyEnum} indicating the literal datatype serialization policy.
     */
    public LiteralDatatypePolicyEnum getLiteralDatatypePolicy() {
        return literalDatatypePolicy;
    }

    /**
     * Checks if non-ASCII characters should be escaped using Unicode escape sequences.
     *
     * @return {@code true} if Unicode escaping is enabled, {@code false} otherwise.
     */
    public boolean escapeUnicode() {
        return escapeUnicode;
    }

    /**
     * Checks if a dot `.` should be added at the end of each triple block or statement.
     *
     * @return {@code true} if a trailing dot is required, {@code false} otherwise.
     */
    public boolean trailingDot() {
        return trailingDot;
    }

    /**
     * Returns the base IRI to be used for the serialization.
     *
     * @return The base IRI string, or {@code null} if no base IRI is specified.
     */
    public String getBaseIRI() {
        return baseIRI;
    }

    /**
     * Checks if deterministic blank node IDs should be generated.
     *
     * @return {@code true} if stable blank node IDs are enabled, {@code false} otherwise.
     */
    public boolean stableBlankNodeIds() {
        return stableBlankNodeIds;
    }

    /**
     * Returns the string used for line endings.
     *
     * @return The line ending string (e.g., `"\n"` for Unix, `"\r\n"` for Windows).
     */
    public String getLineEnding() {
        return lineEnding;
    }

    /**
     * Checks if strict mode for validation is enabled.
     *
     * @return {@code true} if strict mode is enabled, {@code false} otherwise.
     */
    public boolean isStrictMode() {
        return strictMode;
    }

    /**
     * Checks if URIs should be validated for compliance with RDF/serialization rules.
     *
     * @return {@code true} if URI validation is enabled, {@code false} otherwise.
     */
    public boolean validateURIs() {
        return validateURIs;
    }

    /**
     * Checks if context information (named graphs) should be included in the serialization output.
     *
     * @return {@code true} if context inclusion is enabled, {@code false} otherwise.
     */
    public boolean includeContext() {
        return includeContext;
    }

    /**
     * An abstract base builder for {@link AbstractSerializerOption}.
     * This builder provides methods for setting common serialization configuration options.
     * It uses a recursive type parameter (`S`) to allow concrete subclass builders
     * to return their own specific type, enabling fluent API chaining.
     *
     * @param <S> The type of the concrete builder extending this abstract builder.
     */
    public abstract static class AbstractBuilder<S extends AbstractBuilder<S>> {
        protected LiteralDatatypePolicyEnum literalDatatypePolicy = LiteralDatatypePolicyEnum.MINIMAL;
        protected boolean escapeUnicode = false;
        protected boolean trailingDot = true;
        protected String baseIRI = null;
        protected boolean stableBlankNodeIds = false;
        protected String lineEnding = SerializationConstants.DEFAULT_LINE_ENDING;

        protected boolean strictMode = true;
        protected boolean validateURIs = true;
        protected boolean includeContext = false;

        /**
         * Sets the policy for how literal datatypes are printed.
         *
         * @param policy The {@link LiteralDatatypePolicyEnum} to set. Must not be null.
         * @return The builder instance for fluent chaining.
         * @throws NullPointerException if the provided policy is null.
         */
        public S literalDatatypePolicy(LiteralDatatypePolicyEnum policy) {
            this.literalDatatypePolicy = Objects.requireNonNull(policy);
            return self();
        }

        /**
         * Sets whether non-ASCII characters should be escaped using Unicode escape sequences.
         *
         * @param escape {@code true} to enable Unicode escaping, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public S escapeUnicode(boolean escape) {
            this.escapeUnicode = escape;
            return self();
        }

        /**
         * Sets whether a dot `.` should be added at the end of each triple block or statement.
         *
         * @param trailing {@code true} to require a trailing dot, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public S trailingDot(boolean trailing) {
            this.trailingDot = trailing;
            return self();
        }

        /**
         * Sets the base IRI to be used for the serialization.
         *
         * @param base The base IRI string. Can be {@code null}.
         * @return The builder instance for fluent chaining.
         */
        public S baseIRI(String base) {
            this.baseIRI = base;
            return self();
        }

        /**
         * Sets whether deterministic blank node IDs should be generated.
         *
         * @param stable {@code true} to enable stable blank node IDs, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public S stableBlankNodeIds(boolean stable) {
            this.stableBlankNodeIds = stable;
            return self();
        }

        /**
         * Sets the string used for line endings.
         *
         * @param lineEnding The line ending string (e.g., `"\n"` for Unix, `"\r\n"` for Windows). Must not be null.
         * @return The builder instance for fluent chaining.
         * @throws NullPointerException if the provided line ending is null.
         */
        public S lineEnding(String lineEnding) {
            this.lineEnding = Objects.requireNonNull(lineEnding);
            return self();
        }

        /**
         * Sets the general strictness setting for validation during serialization.
         *
         * @param strict {@code true} to enable strict mode, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public S strictMode(boolean strict) {
            this.strictMode = strict;
            return self();
        }

        /**
         * Sets whether URIs should be validated for compliance with RDF/serialization rules.
         *
         * @param validate {@code true} to enable URI validation, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public S validateURIs(boolean validate) {
            this.validateURIs = validate;
            return self();
        }

        /**
         * Sets whether context information (named graphs) should be included in the serialization output.
         *
         * @param include {@code true} to include context information, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public S includeContext(boolean include) {
            this.includeContext = include;
            return self();
        }

        /**
         * Builds and returns a new {@link AbstractSerializerOption} instance with the current builder settings.
         * This method must be implemented by concrete builder subclasses to return their specific configuration type.
         *
         * @return A new {@code AbstractSerializerConfig} instance or a subclass instance.
         */
        public abstract AbstractSerializerOption build();

        /**
         * Helper method to return the concrete builder instance for fluent API chaining.
         * This method is used internally by the builder methods to ensure that method calls
         * return the correct subclass type, allowing for method chaining.
         *
         * @return The concrete builder instance.
         */
        protected final S self() {
            return (S) this;
        }
    }
}
