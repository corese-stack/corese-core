package fr.inria.corese.core.next.data.impl.io.serializer.option;

import fr.inria.corese.core.next.data.api.io.serializer.option.LiteralDatatypePolicy;

/**
 * An abstract base class for serialization configurations of N-Family RDF formats (e.g., N-Triples, N-Quads).
 * This class extends {@link AbstractSerializerOptions} and provides a common foundation
 * for formats that typically have simpler, line-based structures and specific default behaviors
 * regarding literal datatypes and character escaping.
 *
 * <p>It enforces the use of the Builder pattern for construction through its
 * nested {@link AbstractNFamilyBuilder}. Subclasses are expected to extend this
 * configuration and its builder to add format-specific options.</p>
 */
public abstract class AbstractNFamilyOptions extends AbstractSerializerOptions {

    protected final boolean sortSubjects;
    protected final boolean sortPredicates;
    protected final boolean sortObjects;

    /**
     * Protected constructor to be used by concrete builder implementations.
     * Initializes the N-Family serialization configuration options, calling the superclass
     * constructor for common options.
     *
     * @param builder The builder instance containing the desired configuration values.
     */
    protected AbstractNFamilyOptions(AbstractNFamilyBuilder<?> builder) {
        super(builder);
        this.sortSubjects = builder.sortSubjects;
        this.sortPredicates = builder.sortPredicates;
        this.sortObjects = builder.sortObjects;
    }

    public boolean sortSubjects() { return sortSubjects; }
    public boolean sortPredicates() { return sortPredicates; }
    public boolean sortObjects() { return sortObjects; }

    /**
     * Returns {@code true} if any sort key is enabled.
     */
    public boolean sortStatements() {
        return sortSubjects || sortPredicates || sortObjects;
    }

    /**
     * An abstract base builder for {@link AbstractNFamilyOptions}.
     * This builder provides methods for setting N-Family serialization configuration options.
     * It extends {@link AbstractSerializerOptions.AbstractBuilder} and uses a recursive type
     * parameter (`S`) to allow concrete subclass builders to return their own specific type,
     * enabling fluent API chaining.
     *
     * <p>By default, it sets {@code literalDatatypePolicy} to {@link LiteralDatatypePolicy#ALWAYS_TYPED}
     * and {@code escapeUnicode} to {@code true}, which are common characteristics of N-Family formats.</p>
     *
     * @param <S> The type of the concrete builder extending this abstract builder.
     */
    public abstract static class AbstractNFamilyBuilder<S extends AbstractNFamilyBuilder<S>>
            extends AbstractSerializerOptions.AbstractBuilder<S> {

        protected boolean sortSubjects = false;
        protected boolean sortPredicates = false;
        protected boolean sortObjects = false;

        protected AbstractNFamilyBuilder() {
            super.literalDatatypePolicy = LiteralDatatypePolicy.ALWAYS_TYPED;
            super.escapeUnicode = true;
        }

        public S sortSubjects(boolean v) { this.sortSubjects = v; return self(); }
        public S sortPredicates(boolean v) { this.sortPredicates = v; return self(); }
        public S sortObjects(boolean v) { this.sortObjects = v; return self(); }

        /**
         * Convenience method: sets sortSubjects, sortPredicates, and sortObjects to the same value.
         */
        public S sortStatements(boolean v) {
            this.sortSubjects = v;
            this.sortPredicates = v;
            this.sortObjects = v;
            return self();
        }

        /**
         * Builds and returns a new {@link AbstractNFamilyOptions} instance with the current builder settings.
         * This method must be implemented by concrete builder subclasses to return their specific configuration type.
         *
         * @return A new {@code AbstractNFamilyConfig} instance or a subclass instance.
         */
        public abstract AbstractNFamilyOptions build();
    }
}
