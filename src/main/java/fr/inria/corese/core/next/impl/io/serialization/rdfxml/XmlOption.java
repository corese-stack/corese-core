package fr.inria.corese.core.next.impl.io.serialization.rdfxml;

import fr.inria.corese.core.next.impl.io.serialization.option.AbstractSerializerOption;
import fr.inria.corese.core.next.impl.io.serialization.option.LiteralDatatypePolicyEnum;
import fr.inria.corese.core.next.impl.io.serialization.option.PrefixOrderingEnum;
import fr.inria.corese.core.next.impl.io.serialization.util.SerializationConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for RDF/XML serialization format.
 * This class extends {@link AbstractSerializerOption} directly as RDF/XML has
 * distinct serialization characteristics not shared by the Turtle or N-Family formats.
 *
 * <p>Use the {@link Builder} class to create instances of {@code XmlConfig}.
 * A predefined default configuration is available via {@link #defaultConfig()}.</p>
 */
public class XmlOption extends AbstractSerializerOption {

    /**
     * Whether prefix declarations (e.g., `xmlns:prefix="uri"`) should be used for compact IRIs.
     * This is crucial for human-readable formats like RDF/XML.
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
     * Keys are prefixes, values are namespace URIs.
     */
    protected final Map<String, String> customPrefixes;
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
     * Whether subjects should be sorted alphabetically in the output.
     * This ensures a consistent and reproducible order of subjects, useful for diffing or testing.
     */
    protected final boolean sortSubjects;
    /**
     * Whether predicates should be sorted alphabetically within a subject group.
     * This ensures a consistent and reproducible order of properties for a given subject.
     */
    protected final boolean sortPredicates;
    /**
     * Whether multi-line literal syntax (e.g., CDATA sections or direct text nodes with newlines)
     * should be used for literals containing newline characters.
     */
    protected final boolean useMultilineLiterals;


    /**
     * Protected constructor to be used by the {@link Builder}.
     *
     * @param builder The builder instance containing the desired configuration values.
     */
    protected XmlOption(Builder builder) {
        super(builder);

        this.usePrefixes = builder.usePrefixes;
        this.autoDeclarePrefixes = builder.autoDeclarePrefixes;
        this.prefixOrdering = Objects.requireNonNull(builder.prefixOrdering, "Prefix ordering cannot be null");
        this.customPrefixes = Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(builder.customPrefixes, "Custom prefixes map cannot be null")));
        this.prettyPrint = builder.prettyPrint;
        this.indent = Objects.requireNonNull(builder.indent, "Indentation string cannot be null");
        this.maxLineLength = builder.maxLineLength;
        this.sortSubjects = builder.sortSubjects;
        this.sortPredicates = builder.sortPredicates;
        this.useMultilineLiterals = builder.useMultilineLiterals;

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
     * Checks if multi-line literal syntax should be used.
     *
     * @return {@code true} if multi-line literals are enabled, {@code false} otherwise.
     */
    public boolean useMultilineLiterals() {
        return useMultilineLiterals;
    }


    /**
     * Public Builder for {@link XmlOption}.
     * Provides a fluent API for constructing {@code XmlConfig} instances with default values
     * specific to the RDF/XML format.
     */
    public static class Builder extends AbstractSerializerOption.AbstractBuilder<Builder> {
        protected boolean usePrefixes = true;
        protected boolean autoDeclarePrefixes = true;
        protected PrefixOrderingEnum prefixOrdering = PrefixOrderingEnum.ALPHABETICAL;
        protected final Map<String, String> customPrefixes = new HashMap<>();
        protected boolean prettyPrint = true;
        protected String indent = SerializationConstants.DEFAULT_INDENTATION;
        protected int maxLineLength = 0;
        protected boolean sortSubjects = false;
        protected boolean sortPredicates = false;
        protected boolean useMultilineLiterals = true;


        /**
         * Default constructor initializes all options with their default values for RDF/XML.
         */
        public Builder() {
            // Call superclass builder methods for common properties
            literalDatatypePolicy(LiteralDatatypePolicyEnum.ALWAYS_TYPED); // RDF/XML typically types all literals
            trailingDot(false); // No trailing dot in RDF/XML
            stableBlankNodeIds(true); // Good for reproducible RDF/XML outputs
            escapeUnicode(false); // Usually direct UTF-8 for RDF/XML, not unicode escapes

            addCustomPrefix("rdf", SerializationConstants.RDF_NS);
            addCustomPrefix("rdfs", SerializationConstants.RDFS_NS);
            addCustomPrefix("xsd", SerializationConstants.XSD_NS);
            addCustomPrefix("owl", SerializationConstants.OWL_NS);
        }


        /**
         * Sets whether prefix declarations should be used for compact IRIs.
         *
         * @param usePrefixes {@code true} to use prefixes, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public Builder usePrefixes(boolean usePrefixes) {
            this.usePrefixes = usePrefixes;
            return self();
        }

        /**
         * Sets whether the serializer should automatically discover and declare prefixes.
         *
         * @param autoDeclarePrefixes {@code true} to enable auto-declaration, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public Builder autoDeclarePrefixes(boolean autoDeclarePrefixes) {
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
        public Builder prefixOrdering(PrefixOrderingEnum prefixOrdering) {
            this.prefixOrdering = Objects.requireNonNull(prefixOrdering);
            return self();
        }

        /**
         * Adds a custom prefix mapping to be used for serialization.
         *
         * @param prefix    The prefix name (e.g., "ex"). Must not be null.
         * @param namespace The namespace URI Must not be null.
         * @return The builder instance for fluent chaining.
         * @throws NullPointerException if prefix or namespace is null.
         */
        public Builder addCustomPrefix(String prefix, String namespace) {
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
        public Builder addCustomPrefixes(Map<String, String> prefixes) {
            Objects.requireNonNull(prefixes, "Prefixes map cannot be null");
            this.customPrefixes.putAll(prefixes);
            return self();
        }

        /**
         * Sets whether human-readable formatting (pretty-printing) is enabled.
         *
         * @param prettyPrint {@code true} to enable pretty-printing, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public Builder prettyPrint(boolean prettyPrint) {
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
        public Builder indent(String indent) {
            this.indent = Objects.requireNonNull(indent);
            return self();
        }

        /**
         * Sets the maximum desired line length before the serializer attempts to break lines.
         *
         * @param maxLineLength The maximum line length.
         * @return The builder instance for fluent chaining.
         */
        public Builder maxLineLength(int maxLineLength) {
            this.maxLineLength = maxLineLength;
            return self();
        }

        /**
         * Sets whether subjects should be sorted alphabetically in the output.
         *
         * @param sortSubjects {@code true} to enable subject sorting, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public Builder sortSubjects(boolean sortSubjects) {
            this.sortSubjects = sortSubjects;
            return self();
        }

        /**
         * Sets whether predicates should be sorted alphabetically within a subject group.
         *
         * @param sortPredicates {@code true} to enable predicate sorting, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public Builder sortPredicates(boolean sortPredicates) {
            this.sortPredicates = sortPredicates;
            return self();
        }

        /**
         * Sets whether multi-line literal syntax should be used.
         *
         * @param useMultilineLiterals {@code true} to enable multi-line literals, {@code false} otherwise.
         * @return The builder instance for fluent chaining.
         */
        public Builder useMultilineLiterals(boolean useMultilineLiterals) {
            this.useMultilineLiterals = useMultilineLiterals;
            return self();
        }

        /**
         * Builds and returns a new {@link XmlOption} instance with the current builder settings.
         *
         * @return A new {@code XmlConfig} instance.
         */
        @Override
        public XmlOption build() {
            return new XmlOption(this);
        }
    }

    /**
     * Returns a default configuration suitable for RDF/XML serialization.
     * This provides a convenient way to get a standard RDF/XML configuration without
     * manually building it.
     *
     * @return A {@code XmlConfig} instance with default settings.
     */
    public static XmlOption defaultConfig() {
        return new Builder().build();
    }
}