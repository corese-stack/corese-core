package fr.inria.corese.core.next.impl.io.serialization.option;

import fr.inria.corese.core.next.impl.io.serialization.util.SerializationConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An abstract base class for serialization configurations of Turtle Trig RDF formats (e.g., Turtle, TriG).
 * This class extends {@link AbstractSerializerOption} and introduces parameters specific to
 * formats that utilize syntax sugar, pretty-printing, and collection syntax.
 *
 * <p>It enforces the use of the Builder pattern for construction through its
 * nested {@link AbstractTFamilyBuilder}. Subclasses are expected to extend this
 * configuration and its builder to add format-specific options.</p>
 */
public abstract class AbstractTFamilyOption extends AbstractSerializerOption {

    /**
     * Whether prefix declarations (e.g., `@prefix`, `PREFIX`) should be used for compact IRIs.
     * This is crucial for human-readable formats like Turtle but not for N-Triples.
     */
    protected final boolean usePrefixes;
    /**
     * Whether the serializer should automatically discover and declare prefixes used in the graph.
     * This avoids manual prefix configuration but can lead to more prefixes than strictly needed.
     */
    protected final boolean autoDeclarePrefixes;
    /**
     * The policy for ordering prefix declarations (e.g., alphabetically, by usage, or custom).
     * This impacts the determinism and readability of the prefix block.
     */
    protected final PrefixOrderingEnum prefixOrdering;
    /**
     * A map of custom URI prefixes to be used for serialization, in addition to or instead of
     * auto-declared prefixes. Useful for enforcing specific prefix names or when {@code autoDeclarePrefixes} is false.
     */
    protected final Map<String, String> customPrefixes; // Used for CUSTOM ordering or if autoDeclarePrefixes=false
    /**
     * Whether compact triple syntax (e.g., using ';' for subject/predicate reuse and ',' for object lists)
     * should be used. This significantly reduces file size and improves readability for formats like Turtle.
     */
    protected final boolean useCompactTriples; // Includes comma-separated objects and subject/predicate reuse via ';'
    /**
     * Whether the `a` shortcut should be used for `rdf:type` predicates.
     * This is a common Turtle shorthand that improves conciseness and readability.
     */
    protected final boolean useRdfTypeShortcut; // 'a' instead of 'rdf:type'
    /**
     * Whether Turtle collection syntax `( item1 item2 )` should be used for `rdf:List` structures.
     * This provides a more idiomatic and readable representation of lists in Turtle.
     */
    protected final boolean useCollections; // Turtle collection syntax ( )
    /**
     * The preferred style for serializing blank nodes (e.g., `[]` vs `_:id`).
     * This affects both the conciseness and the identifiability of blank nodes in the output.
     */
    protected final BlankNodeStyleEnum blankNodeStyle; // [] vs _:id
    /**
     * Whether multi-line literal syntax (triple quotes `"""..."""`) should be used for literals
     * containing newline characters.
     */
    protected final boolean useMultilineLiterals;

    // --- Pretty-Printing Options ---
    /**
     * Whether human-readable formatting with indentation and newlines (pretty-printing) is enabled.
     * This makes the output easier for humans to read and debug, but increases file size slightly.
     */
    protected final boolean prettyPrint;
    /**
     * The string used for indentation (e.g., "  ", "\t").
     * This defines the visual spacing for nested structures when pretty-printing.
     */
    protected final String indent;
    /**
     * The maximum desired line length before the serializer attempts to break lines.
     * This helps ensure readability by preventing very long lines in the output.
     */
    protected final int maxLineLength;
    /**
     * Whether triples should be grouped by subject in the output (e.g., using ';' and '.').
     * This organizes the output logically around subjects, improving readability.
     */
    protected final boolean groupBySubject; // Group triples by subject using ; and .
    /**
     * Whether subjects should be sorted alphabetically in the output.
     * This ensures a consistent and reproducible order of subjects, useful for diffing or testing.
     */
    protected final boolean sortSubjects; // Sort subjects alphabetically
    /**
     * Whether predicates should be sorted alphabetically within a subject group.
     * This ensures a consistent and reproducible order of properties for a given subject.
     */
    protected final boolean sortPredicates; // Sort predicates alphabetically within a subject group

    /**
     * Protected constructor to be used by concrete builder implementations.
     * Initializes the Turtle Trig serialization configuration options, calling the superclass
     * constructor for common options.
     *
     * @param builder The builder instance containing the desired configuration values.
     * @throws NullPointerException     if any required field from the builder is null.
     * @throws IllegalArgumentException if incompatible options (e.g., escapeUnicode and useMultilineLiterals) are enabled.
     */
    protected AbstractTFamilyOption(AbstractTFamilyBuilder<?> builder) {
        super(builder);

        this.usePrefixes = builder.usePrefixes;
        this.autoDeclarePrefixes = builder.autoDeclarePrefixes;
        this.prefixOrdering = Objects.requireNonNull(builder.prefixOrdering, "Prefix ordering cannot be null");
        this.customPrefixes = Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(builder.customPrefixes, "Custom prefixes map cannot be null")));
        this.useCompactTriples = builder.useCompactTriples;
        this.useRdfTypeShortcut = builder.useRdfTypeShortcut;
        this.useCollections = builder.useCollections;
        this.blankNodeStyle = Objects.requireNonNull(builder.blankNodeStyle, "Blank node style cannot be null");
        this.useMultilineLiterals = builder.useMultilineLiterals;

        this.prettyPrint = builder.prettyPrint;
        this.indent = Objects.requireNonNull(builder.indent, "Indentation string cannot be null");
        this.maxLineLength = builder.maxLineLength;
        this.groupBySubject = builder.groupBySubject;
        this.sortSubjects = builder.sortSubjects;
        this.sortPredicates = builder.sortPredicates;

        if (this.escapeUnicode() && this.useMultilineLiterals) {
            throw new IllegalArgumentException("Cannot enable both escapeUnicode and useMultilineLiterals in Turtle TriG configs.");
        }
    }


    /**
     * Checks if prefix declarations should be used for compact IRIs.
     *
     * @return {@code true} if prefixes are used, {@code false} otherwise.
     */
    public boolean usePrefixes() {
        return usePrefixes;
    }

    /**
     * Checks if the serializer should automatically discover and declare prefixes.
     *
     * @return {@code true} if auto-declaration is enabled, {@code false} otherwise.
     */
    public boolean autoDeclarePrefixes() {
        return autoDeclarePrefixes;
    }

    /**
     * Returns the policy for ordering prefix declarations.
     *
     * @return The {@link PrefixOrderingEnum} for prefix ordering.
     */
    public PrefixOrderingEnum getPrefixOrdering() {
        return prefixOrdering;
    }

    /**
     * Returns an unmodifiable map of custom URI prefixes.
     *
     * @return A map where keys are prefix names and values are namespace URIs.
     */
    public Map<String, String> getCustomPrefixes() {
        return customPrefixes;
    }

    /**
     * Checks if compact triple syntax (using ';' and ',') should be used.
     *
     * @return {@code true} if compact triples are enabled, {@code false} otherwise.
     */
    public boolean useCompactTriples() {
        return useCompactTriples;
    }

    /**
     * Checks if the `a` shortcut should be used for `rdf:type` predicates.
     *
     * @return {@code true} if the `a` shortcut is enabled, {@code false} otherwise.
     */
    public boolean useRdfTypeShortcut() {
        return useRdfTypeShortcut;
    }

    /**
     * Checks if Turtle collection syntax `( item1 item2 )` should be used for `rdf:List` structures.
     *
     * @return {@code true} if collection syntax is enabled, {@code false} otherwise.
     */
    public boolean useCollections() {
        return useCollections;
    }

    /**
     * Returns the preferred style for serializing blank nodes.
     *
     * @return The {@link BlankNodeStyleEnum} for blank node serialization.
     */
    public BlankNodeStyleEnum getBlankNodeStyle() {
        return blankNodeStyle;
    }

    /**
     * Checks if multi-line literal syntax (triple quotes) should be used.
     *
     * @return {@code true} if multi-line literals are enabled, {@code false} otherwise.
     */
    public boolean useMultilineLiterals() {
        return useMultilineLiterals;
    }

    /**
     * Checks if human-readable formatting (pretty-printing) is enabled.
     *
     * @return {@code true} if pretty-printing is enabled, {@code false} otherwise.
     */
    public boolean prettyPrint() {
        return prettyPrint;
    }

    /**
     * Returns the string used for indentation when pretty-printing.
     *
     * @return The indentation string.
     */
    public String getIndent() {
        return indent;
    }

    /**
     * Returns the maximum desired line length before the serializer attempts to break lines.
     *
     * @return The maximum line length.
     */
    public int getMaxLineLength() {
        return maxLineLength;
    }

    /**
     * Checks if triples should be grouped by subject in the output.
     *
     * @return {@code true} if grouping by subject is enabled, {@code false} otherwise.
     */
    public boolean groupBySubject() {
        return groupBySubject;
    }

    /**
     * Checks if subjects should be sorted alphabetically in the output.
     *
     * @return {@code true} if subject sorting is enabled, {@code false} otherwise.
     */
    public boolean sortSubjects() {
        return sortSubjects;
    }

    /**
     * Checks if predicates should be sorted alphabetically within a subject group.
     *
     * @return {@code true} if predicate sorting is enabled, {@code false} otherwise.
     */
    public boolean sortPredicates() {
        return sortPredicates;
    }

    /**
     * Determines if triple quotes should be used for a given literal value.
     * This is typically true if multi-line literals are enabled and the value contains newline characters.
     *
     * @param literalValue The string value of the literal.
     * @return {@code true} if triple quotes should be used, {@code false} otherwise.
     */
    public boolean shouldUseTripleQuotes(String literalValue) {
        return useMultilineLiterals && (literalValue.contains(SerializationConstants.LINE_FEED) || literalValue.contains(SerializationConstants.CARRIAGE_RETURN));
    }

    /**
     * Checks if output optimization features (compact triples, subject grouping, pretty-printing) are enabled.
     *
     * @return {@code true} if any optimization feature is enabled, {@code false} otherwise.
     */
    public boolean shouldOptimizeOutput() {
        return useCompactTriples || groupBySubject || prettyPrint;
    }

    /**
     * Checks if inline blank node syntax (`[]`) should be used.
     * This is typically true if anonymous blank node style is chosen and compact triples are enabled.
     *
     * @return {@code true} if inline blank nodes should be used, {@code false} otherwise.
     */
    public boolean shouldUseInlineBlankNodes() {
        return blankNodeStyle == BlankNodeStyleEnum.ANONYMOUS && useCompactTriples;
    }

    /**
     * An abstract base builder for {@link AbstractTFamilyOption}.
     * This builder provides methods for setting Turtle Trig serialization configuration options.
     * It extends {@link AbstractSerializerOption.AbstractBuilder} and uses a recursive type
     * parameter (`S`) to allow concrete subclass builders to return their own specific type,
     * enabling fluent API chaining.
     *
     * @param <S> The type of the concrete builder extending this abstract builder.
     */
    public abstract static class AbstractTFamilyBuilder<S extends AbstractTFamilyBuilder<S>>
            extends AbstractSerializerOption.AbstractBuilder<S> {

        protected boolean usePrefixes = true;
        protected boolean autoDeclarePrefixes = true;
        protected PrefixOrderingEnum prefixOrdering = PrefixOrderingEnum.ALPHABETICAL;
        protected final Map<String, String> customPrefixes = new HashMap<>();

        protected boolean useCompactTriples = true;
        protected boolean useRdfTypeShortcut = true;
        // Default to false for complexity, specific formats can override
        protected boolean useCollections = false;
        // Default to NAMED (safer for initial impl), specific formats can override
        protected BlankNodeStyleEnum blankNodeStyle = BlankNodeStyleEnum.NAMED;
        protected boolean useMultilineLiterals = true;

        // Pretty-Printing Defaults
        protected boolean prettyPrint = true;
        protected String indent = SerializationConstants.DEFAULT_INDENTATION;
        protected int maxLineLength = 80;
        protected boolean groupBySubject = true;
        protected boolean sortSubjects = false;
        protected boolean sortPredicates = false;

        /**
         * Sets whether prefix declarations should be used for compact IRIs.
         *
         * @param usePrefixes {@code true} to use prefixes, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public S usePrefixes(boolean usePrefixes) {
            this.usePrefixes = usePrefixes;
            return self();
        }

        /**
         * Sets whether the serializer should automatically discover and declare prefixes.
         *
         * @param autoDeclarePrefixes {@code true} to enable auto-declaration, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public S autoDeclarePrefixes(boolean autoDeclarePrefixes) {
            this.autoDeclarePrefixes = autoDeclarePrefixes;
            return self();
        }

        /**
         * Sets the policy for ordering prefix declarations.
         *
         * @param prefixOrdering The {@link PrefixOrderingEnum} to set. Must not be null.
         * @return The builder instance for fluent chaining.
         * @throws NullPointerException if the provided policy is null.
         */
        public S prefixOrdering(PrefixOrderingEnum prefixOrdering) {
            this.prefixOrdering = Objects.requireNonNull(prefixOrdering);
            return self();
        }

        /**
         * Adds a custom prefix mapping to be used for serialization.
         *
         * @param prefix    The prefix name (e.g., "ex"). Must not be null.
         * @param namespace The namespace URI (e.g., "http://example.org/"). Must not be null.
         * @return The builder instance for fluent chaining.
         * @throws NullPointerException if prefix or namespace is null.
         */
        public S addCustomPrefix(String prefix, String namespace) {
            Objects.requireNonNull(prefix, "Prefix name cannot be null");
            Objects.requireNonNull(namespace, "Namespace URI cannot be null");
            this.customPrefixes.put(prefix, namespace);
            return self();
        }

        /**
         * Adds multiple custom prefix mappings from a map.
         *
         * @param prefixes A map of prefix names to namespace URIs. Must not be null.
         * @return The builder instance for fluent chaining.
         * @throws NullPointerException if the provided map is null.
         */
        public S addCustomPrefixes(Map<String, String> prefixes) {
            Objects.requireNonNull(prefixes, "Prefixes map cannot be null");
            this.customPrefixes.putAll(prefixes);
            return self();
        }

        /**
         * Sets whether compact triple syntax (using ';' and ',') should be used.
         *
         * @param useCompactTriples {@code true} to enable compact triples, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public S useCompactTriples(boolean useCompactTriples) {
            this.useCompactTriples = useCompactTriples;
            return self();
        }

        /**
         * Sets whether the `a` shortcut should be used for `rdf:type` predicates.
         *
         * @param useRdfTypeShortcut {@code true} to enable the `a` shortcut, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public S useRdfTypeShortcut(boolean useRdfTypeShortcut) {
            this.useRdfTypeShortcut = useRdfTypeShortcut;
            return self();
        }

        /**
         * Sets whether Turtle collection syntax `( item1 item2 )` should be used for `rdf:List` structures.
         *
         * @param useCollections {@code true} to enable collection syntax, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public S useCollections(boolean useCollections) {
            this.useCollections = useCollections;
            return self();
        }

        /**
         * Sets the preferred style for serializing blank nodes.
         *
         * @param blankNodeStyle The {@link BlankNodeStyleEnum} to set. Must not be null.
         * @return The builder instance for fluent chaining.
         * @throws NullPointerException if the provided style is null.
         */
        public S blankNodeStyle(BlankNodeStyleEnum blankNodeStyle) {
            this.blankNodeStyle = Objects.requireNonNull(blankNodeStyle);
            return self();
        }

        /**
         * Sets whether multi-line literal syntax (triple quotes) should be used.
         *
         * @param useMultilineLiterals {@code true} to enable multi-line literals, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public S useMultilineLiterals(boolean useMultilineLiterals) {
            this.useMultilineLiterals = useMultilineLiterals;
            return self();
        }

        /**
         * Sets whether human-readable formatting (pretty-printing) is enabled.
         *
         * @param prettyPrint {@code true} to enable pretty-printing, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public S prettyPrint(boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
            return self();
        }

        /**
         * Sets the string used for indentation when pretty-printing.
         *
         * @param indent The indentation string. Must not be null.
         * @return The builder instance for fluent chaining.
         * @throws NullPointerException if the provided indent string is null.
         */
        public S indent(String indent) {
            this.indent = Objects.requireNonNull(indent);
            return self();
        }

        /**
         * Sets the maximum desired line length before the serializer attempts to break lines.
         *
         * @param maxLineLength The maximum line length.
         * @return The builder instance for fluent chaining.
         */
        public S maxLineLength(int maxLineLength) {
            this.maxLineLength = maxLineLength;
            return self();
        }

        /**
         * Sets whether triples should be grouped by subject in the output.
         *
         * @param groupBySubject {@code true} to enable grouping by subject, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public S groupBySubject(boolean groupBySubject) {
            this.groupBySubject = groupBySubject;
            return self();
        }

        /**
         * Sets whether subjects should be sorted alphabetically in the output.
         *
         * @param sortSubjects {@code true} to enable subject sorting, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public S sortSubjects(boolean sortSubjects) {
            this.sortSubjects = sortSubjects;
            return self();
        }

        /**
         * Sets whether predicates should be sorted alphabetically within a subject group.
         *
         * @param sortPredicates {@code true} to enable predicate sorting, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public S sortPredicates(boolean sortPredicates) {
            this.sortPredicates = sortPredicates;
            return self();
        }

        /**
         * Builds and returns a new {@link AbstractTFamilyOption} instance with the current builder settings.
         * This method must be implemented by concrete builder subclasses to return their specific configuration type.
         *
         * @return A new {@code AbstractTFamilyConfig} instance or a subclass instance.
         */
        public abstract AbstractTFamilyOption build();
    }
}
