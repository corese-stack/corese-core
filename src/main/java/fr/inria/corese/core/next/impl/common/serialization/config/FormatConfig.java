package fr.inria.corese.core.next.impl.common.serialization.config;

import fr.inria.corese.core.next.impl.common.util.SerializationConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for RDF serialization formats (Turtle, TriG, N-Triples, N-Quads).
 * This class provides a comprehensive set of options to control the output
 * syntax, pretty-printing, and technical aspects of RDF serialization.
 *
 * <p>Use the {@link Builder} class to create instances of {@code FormatConfig}.
 * Predefined configurations for common RDF formats are available via static methods
 * like {@link #ntriplesConfig()}, {@link #nquadsConfig()}, {@link #turtleConfig()}, etc.</p>
 */
public class FormatConfig {

    /**
     * Whether prefix declarations (e.g., `@prefix`, `PREFIX`) should be used for compact IRIs.
     * This is crucial for human-readable formats like Turtle but not for N-Triples.
     */
    private final boolean usePrefixes;
    /**
     * Whether the serializer should automatically discover and declare prefixes used in the graph.
     * This avoids manual prefix configuration but can lead to more prefixes than strictly needed.
     */
    private final boolean autoDeclarePrefixes;
    /**
     * The policy for ordering prefix declarations (e.g., alphabetically, by usage, or custom).
     * This impacts the determinism and readability of the prefix block.
     */
    private final PrefixOrderingEnum prefixOrdering;
    /**
     * A map of custom URI prefixes to be used for serialization, in addition to or instead of
     * auto-declared prefixes. Useful for enforcing specific prefix names or when {@code autoDeclarePrefixes} is false.
     */
    private final Map<String, String> customPrefixes; // Used for CUSTOM ordering or if autoDeclarePrefixes=false

    /**
     * Whether compact triple syntax (e.g., using ';' for subject/predicate reuse and ',' for object lists)
     * should be used. This significantly reduces file size and improves readability for formats like Turtle.
     */
    private final boolean useCompactTriples; // Includes comma-separated objects and subject/predicate reuse via ';'
    /**
     * Whether the `a` shortcut should be used for `rdf:type` predicates.
     * This is a common Turtle shorthand that improves conciseness and readability.
     */
    private final boolean useRdfTypeShortcut; // 'a' instead of 'rdf:type'
    /**
     * Whether Turtle collection syntax `( item1 item2 )` should be used for `rdf:List` structures.
     * This provides a more idiomatic and readable representation of lists in Turtle.
     */
    private final boolean useCollections; // Turtle collection syntax ( )
    /**
     * The preferred style for serializing blank nodes (e.g., `[]` vs `_:id`).
     * This affects both the conciseness and the identifiability of blank nodes in the output.
     */
    private final BlankNodeStyleEnum blankNodeStyle; // [] vs _:id

    // --- Pretty-Printing Options ---
    /**
     * Whether human-readable formatting with indentation and newlines (pretty-printing) is enabled.
     * This makes the output easier for humans to read and debug, but increases file size slightly.
     */
    private final boolean prettyPrint;
    /**
     * The string used for indentation (e.g., "  ", "\t").
     * This defines the visual spacing for nested structures when pretty-printing.
     */
    private final String indent;
    /**
     * The maximum desired line length before the serializer attempts to break lines.
     * This helps ensure readability by preventing very long lines in the output.
     */
    private final int maxLineLength;
    /**
     * Whether triples should be grouped by subject in the output (e.g., using ';' and '.').
     * This organizes the output logically around subjects, improving readability.
     */
    private final boolean groupBySubject; // Group triples by subject using ; and .
    /**
     * Whether subjects should be sorted alphabetically in the output.
     * This ensures a consistent and reproducible order of subjects, useful for diffing or testing.
     */
    private final boolean sortSubjects; // Sort subjects alphabetically
    /**
     * Whether predicates should be sorted alphabetically within a subject group.
     * This ensures a consistent and reproducible order of properties for a given subject.
     */
    private final boolean sortPredicates; // Sort predicates alphabetically within a subject group

    // --- Technical Output Options ---
    /**
     * The policy for how literal datatypes are printed.
     * This determines whether datatypes are always explicit, minimal, or follow specific rules.
     */
    private final LiteralDatatypePolicyEnum literalDatatypePolicy;
    /**
     * Whether non-ASCII characters should be escaped using Unicode escape sequences (e.g., `\u00E9`).
     * This ensures compatibility with systems that might not handle UTF-8 correctly, but makes output less human-readable.
     */
    private final boolean escapeUnicode; // XXXX for non-ASCII
    /**
     * Whether a dot `.` should be added at the end of each triple block or statement.
     * This is a syntax requirement for some RDF serialization formats (e.g., Turtle, N-Triples).
     */
    private final boolean trailingDot; // Add a dot '.' at the end of each triple block
    /**
     * The base IRI to be used for the serialization.
     * This allows relative IRIs to be resolved and can shorten the output by avoiding full IRIs.
     */
    private final String baseIRI; // @base directive
    /**
     * Whether deterministic blank node IDs (e.g., `_:b0`, `_:b1`) should be generated.
     * This is crucial for reproducible outputs, especially in testing environments, as blank node IDs are typically random.
     */
    private final boolean stableBlankNodeIds; // Generate deterministic _:bids

    // --- Validation & Context Options ---
    /**
     * General strictness setting for validation during serialization.
     * Enabling this can catch errors or non-standard RDF constructs but might reject valid, less common patterns.
     */
    private final boolean strictMode; // General strictness for validation
    /**
     * Whether URIs should be validated for compliance with RDF/Turtle/N-Triples rules.
     * This ensures that generated URIs are valid and will be correctly parsed by other tools.
     */
    private final boolean validateURIs; // Specific URI validation
    /**
     * Whether context information (named graphs) should be included in the serialization output.
     * This is essential for formats like N-Quads or TriG which support named graphs.
     */
    private final boolean includeContext; // For N-Quads (control writing the 4th element)
    /**
     * The string used for line endings (e.g., `"\n"` for Unix, `"\r\n"` for Windows).
     * This ensures that the generated file has correct line endings for the target operating system.
     */
    private final String lineEnding;


    private final boolean useMultilineLiterals;

    /**
     * Private constructor to enforce usage of the Builder.
     *
     * @param builder The builder instance.
     */
    private FormatConfig(Builder builder) {

        this.usePrefixes = builder.usePrefixes;
        this.autoDeclarePrefixes = builder.autoDeclarePrefixes;
        this.prefixOrdering = Objects.requireNonNull(builder.prefixOrdering, "Prefix ordering cannot be null");
        this.customPrefixes = Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(builder.customPrefixes, "Custom prefixes map cannot be null")));

        this.useCompactTriples = builder.useCompactTriples;
        this.useRdfTypeShortcut = builder.useRdfTypeShortcut;
        this.useCollections = builder.useCollections;
        this.blankNodeStyle = Objects.requireNonNull(builder.blankNodeStyle, "Blank node style cannot be null");

        // Pretty-Printing
        this.prettyPrint = builder.prettyPrint;
        this.indent = Objects.requireNonNull(builder.indent, "Indentation string cannot be null");
        this.maxLineLength = builder.maxLineLength;
        this.groupBySubject = builder.groupBySubject;
        this.sortSubjects = builder.sortSubjects;
        this.sortPredicates = builder.sortPredicates;

        // Technical Output
        this.literalDatatypePolicy = Objects.requireNonNull(builder.literalDatatypePolicy, "Literal datatype policy cannot be null");
        this.escapeUnicode = builder.escapeUnicode;
        this.trailingDot = builder.trailingDot;
        this.baseIRI = builder.baseIRI; // Can be null
        this.stableBlankNodeIds = builder.stableBlankNodeIds;

        // Validation & Context
        this.strictMode = builder.strictMode;
        this.validateURIs = builder.validateURIs;
        this.includeContext = builder.includeContext;
        this.lineEnding = Objects.requireNonNull(builder.lineEnding, "Line ending cannot be null");

        if (builder.escapeUnicode && builder.useMultilineLiterals) {
            throw new IllegalArgumentException("Cannot enable both escapeUnicode and useMultilineLiterals");
        }
        this.useMultilineLiterals = builder.useMultilineLiterals;
    }


    // --- Builder Class ---

    /**
     * Builder class for {@link FormatConfig}.
     * Provides a fluent API for constructing FormatConfig instances with default values.
     */
    public static class Builder {
        // Syntax Sugar Defaults
        private boolean usePrefixes = true;
        private boolean autoDeclarePrefixes = true;
        private PrefixOrderingEnum prefixOrdering = PrefixOrderingEnum.ALPHABETICAL;
        private Map<String, String> customPrefixes = new HashMap<>();

        private boolean useCompactTriples = true;
        private boolean useRdfTypeShortcut = true;
        // Default to false for complexity
        private boolean useCollections = false;
        // Default to NAMED (safer for initial impl)
        private BlankNodeStyleEnum blankNodeStyle = BlankNodeStyleEnum.NAMED;

        // Pretty-Printing Defaults
        private boolean prettyPrint = true;
        private String indent = SerializationConstants.DEFAULT_INDENTATION;
        private int maxLineLength = 80;
        private boolean groupBySubject = true;
        private boolean sortSubjects = false;
        private boolean sortPredicates = false;

        // Technical Output Defaults
        private LiteralDatatypePolicyEnum literalDatatypePolicy = LiteralDatatypePolicyEnum.MINIMAL;
        private boolean escapeUnicode = false;
        private boolean trailingDot = true;
        private String baseIRI = null;
        private boolean stableBlankNodeIds = false;

        // Validation & Context Defaults
        private boolean strictMode = true;
        private boolean validateURIs = true;
        private boolean includeContext = false;
        private String lineEnding = SerializationConstants.DEFAULT_LINE_ENDING;
        private boolean useMultilineLiterals = true;

        /**
         * Default constructor initializes all options with their default values.
         * The values are directly assigned during field declaration above.
         */
        public Builder() {
            // No initialized
        }

        // --- Builder Methods for Syntax Sugar Options ---

        public Builder usePrefixes(boolean usePrefixes) {
            this.usePrefixes = usePrefixes;
            return this;
        }

        public Builder autoDeclarePrefixes(boolean autoDeclarePrefixes) {
            this.autoDeclarePrefixes = autoDeclarePrefixes;
            return this;
        }

        public Builder prefixOrdering(PrefixOrderingEnum prefixOrdering) {
            this.prefixOrdering = Objects.requireNonNull(prefixOrdering, "Prefix ordering cannot be null");
            return this;
        }

        public Builder addCustomPrefix(String prefix, String namespace) {
            Objects.requireNonNull(prefix, "Prefix name cannot be null");
            Objects.requireNonNull(namespace, "Namespace URI cannot be null");
            this.customPrefixes.put(prefix, namespace);
            return this;
        }

        public Builder addCustomPrefixes(Map<String, String> prefixes) {
            Objects.requireNonNull(prefixes, "Prefixes map cannot be null");
            this.customPrefixes.putAll(prefixes);
            return this;
        }

        public Builder useCompactTriples(boolean useCompactTriples) {
            this.useCompactTriples = useCompactTriples;
            return this;
        }

        public Builder useRdfTypeShortcut(boolean useRdfTypeShortcut) {
            this.useRdfTypeShortcut = useRdfTypeShortcut;
            return this;
        }

        public Builder useCollections(boolean useCollections) {
            this.useCollections = useCollections;
            return this;
        }

        public Builder blankNodeStyle(BlankNodeStyleEnum blankNodeStyle) {
            this.blankNodeStyle = Objects.requireNonNull(blankNodeStyle, "Blank node style cannot be null");
            return this;
        }

        // --- Builder Methods for Pretty-Printing Options ---

        public Builder prettyPrint(boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
            return this;
        }

        public Builder indent(String indent) {
            this.indent = Objects.requireNonNull(indent, "Indentation string cannot be null");
            return this;
        }

        public Builder maxLineLength(int maxLineLength) {
            this.maxLineLength = maxLineLength;
            return this;
        }

        public Builder groupBySubject(boolean groupBySubject) {
            this.groupBySubject = groupBySubject;
            return this;
        }

        public Builder sortSubjects(boolean sortSubjects) {
            this.sortSubjects = sortSubjects;
            return this;
        }

        public Builder sortPredicates(boolean sortPredicates) {
            this.sortPredicates = sortPredicates;
            return this;
        }

        public Builder useMultilineLiterals(boolean useMultilineLiterals) {
            this.useMultilineLiterals = useMultilineLiterals;
            return this;
        }

        // --- Builder Methods for Technical Output Options ---

        public Builder literalDatatypePolicy(LiteralDatatypePolicyEnum literalDatatypePolicy) {
            this.literalDatatypePolicy = Objects.requireNonNull(literalDatatypePolicy, "Literal datatype policy cannot be null");
            return this;
        }

        public Builder escapeUnicode(boolean escapeUnicode) {
            this.escapeUnicode = escapeUnicode;
            return this;
        }

        public Builder trailingDot(boolean trailingDot) {
            this.trailingDot = trailingDot;
            return this;
        }

        public Builder baseIRI(String baseIRI) {
            this.baseIRI = baseIRI;
            return this;
        } // Can be null

        public Builder stableBlankNodeIds(boolean stableBlankNodeIds) {
            this.stableBlankNodeIds = stableBlankNodeIds;
            return this;
        }

        // --- Builder Methods for Validation & Context Options ---

        public Builder strictMode(boolean strictMode) {
            this.strictMode = strictMode;
            return this;
        }

        public Builder validateURIs(boolean validateURIs) {
            this.validateURIs = validateURIs;
            return this;
        }

        public Builder includeContext(boolean includeContext) {
            this.includeContext = includeContext;
            return this;
        }

        public Builder lineEnding(String lineEnding) {
            this.lineEnding = Objects.requireNonNull(lineEnding, "Line ending cannot be null");
            return this;
        }

        /**
         * Builds and returns a new {@link FormatConfig} instance with the current builder settings.
         *
         * @return A new {@code FormatConfig} instance.
         * @throws NullPointerException if any required field has not been set (should not happen with default values).
         */
        public FormatConfig build() {
            return new FormatConfig(this);
        }
    }

    // --- Predefined Configurations ---

    /**
     * Returns a default configuration suitable for N-Triples serialization.
     * N-Triples is a simple, line-oriented format without prefixes, contexts, or complex syntax sugar.
     *
     * @return A {@code FormatConfig} instance for N-Triples.
     */
    public static FormatConfig ntriplesConfig() {
        return new Builder()
                .usePrefixes(false) // N-Triples doesn't use prefixes
                .autoDeclarePrefixes(false)
                .useCompactTriples(false) // N-Triples is one triple per line
                .useRdfTypeShortcut(false)
                .useCollections(false)
                .blankNodeStyle(BlankNodeStyleEnum.NAMED) // N-Triples uses _:bnodeId
                .prettyPrint(false) // N-Triples is usually not "pretty-printed" beyond newlines
                .indent(SerializationConstants.EMPTY_STRING) // No indentation
                .maxLineLength(0) // No line length limit for simplicity (or can be set very high)
                .groupBySubject(false) // Each triple on its own line
                .sortSubjects(false) // Order not strictly defined
                .sortPredicates(false) // Order not strictly defined
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.ALWAYS_TYPED) // N-Triples typically shows all datatypes explicitly
                .escapeUnicode(true) // N-Triples often requires unicode escaping
                .trailingDot(true)
                .baseIRI(null) // No @base in N-Triples
                .stableBlankNodeIds(true) // Good for reproducible N-Triples tests
                .strictMode(true) // Be strict for N-Triples validation
                .validateURIs(true)
                .useMultilineLiterals(false)
                .includeContext(false) // N-Triples does not support contexts
                .lineEnding(SerializationConstants.DEFAULT_LINE_ENDING)
                .build();
    }

    /**
     * Returns a default configuration suitable for N-Quads serialization.
     * N-Quads extends N-Triples with named graphs.
     *
     * @return A {@code FormatConfig} instance for N-Quads.
     */
    public static FormatConfig nquadsConfig() {
        return new Builder()
                .usePrefixes(false) // N-Quads doesn't use prefixes
                .autoDeclarePrefixes(false)
                .useCompactTriples(false)
                .useRdfTypeShortcut(false)
                .useCollections(false)
                .blankNodeStyle(BlankNodeStyleEnum.NAMED)
                .prettyPrint(false)
                .indent(SerializationConstants.EMPTY_STRING)
                .maxLineLength(0)
                .groupBySubject(false)
                .sortSubjects(false)
                .sortPredicates(false)
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.ALWAYS_TYPED)
                .escapeUnicode(true)
                .trailingDot(true)
                .baseIRI(null)
                .stableBlankNodeIds(true)
                .strictMode(true)
                .validateURIs(true)
                .useMultilineLiterals(false)
                .includeContext(true) // N-Quads includes contexts by definition
                .lineEnding(SerializationConstants.DEFAULT_LINE_ENDING)
                .build();
    }

    /**
     * Returns a default configuration suitable for Turtle serialization.
     * Turtle is a concise, human-readable format with extensive syntax sugar and pretty-printing.
     *
     * @return A {@code FormatConfig} instance for Turtle.
     */
    public static FormatConfig turtleConfig() {
        Map<String, String> commonTurtlePrefixes = new HashMap<>();
        commonTurtlePrefixes.put("rdf", SerializationConstants.RDF_NS);
        commonTurtlePrefixes.put("rdfs", SerializationConstants.RDFS_NS);
        commonTurtlePrefixes.put("xsd", SerializationConstants.XSD_NS);
        commonTurtlePrefixes.put("owl", SerializationConstants.OWL_NS);

        return new Builder()
                .usePrefixes(true)
                .autoDeclarePrefixes(true) // Auto-declare new prefixes found in the graph
                .prefixOrdering(PrefixOrderingEnum.ALPHABETICAL)
                .addCustomPrefixes(commonTurtlePrefixes) // Start with common prefixes
                .useCompactTriples(true) // Enable ; and ,
                .useRdfTypeShortcut(true) // Use 'a'
                .useCollections(true) // Changed to true for comprehensive Turtle output
                .blankNodeStyle(BlankNodeStyleEnum.ANONYMOUS)
                .prettyPrint(true)
                .indent(SerializationConstants.DEFAULT_INDENTATION)
                .maxLineLength(80) // Standard line length
                .groupBySubject(true)
                .sortSubjects(false) // Optional, for reproducible output
                .sortPredicates(false) // Optional, for reproducible output
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.MINIMAL) // Turtle's typical literal style
                .escapeUnicode(false) // Usually direct UTF-8 for Turtle
                .trailingDot(true)
                .baseIRI(null) // No default base IRI
                .stableBlankNodeIds(false) // Random by default
                .strictMode(true)
                .validateURIs(true)
                .includeContext(false) // Turtle does not support contexts
                .useMultilineLiterals(true)
                .lineEnding(SerializationConstants.DEFAULT_LINE_ENDING)
                .build();
    }

    /**
     * Returns a default configuration suitable for TriG serialization.
     * TriG extends Turtle with named graphs, supporting all Turtle features plus context inclusion.
     *
     * @return A {@code FormatConfig} instance for TriG.
     */
    public static FormatConfig trigConfig() {
        Map<String, String> commonTriGPrefixes = new HashMap<>();
        commonTriGPrefixes.put("rdf", SerializationConstants.RDF_NS);
        commonTriGPrefixes.put("rdfs", SerializationConstants.RDFS_NS);
        commonTriGPrefixes.put("xsd", SerializationConstants.XSD_NS);
        commonTriGPrefixes.put("owl", SerializationConstants.OWL_NS);

        return new Builder()
                .usePrefixes(true)
                .autoDeclarePrefixes(true)
                .prefixOrdering(PrefixOrderingEnum.ALPHABETICAL)
                .addCustomPrefixes(commonTriGPrefixes)
                .useCompactTriples(true)
                .useRdfTypeShortcut(true)
                .useCollections(false)
                .blankNodeStyle(BlankNodeStyleEnum.NAMED)
                .prettyPrint(true)
                .indent(SerializationConstants.DEFAULT_INDENTATION)
                .maxLineLength(80)
                .groupBySubject(true)
                .sortSubjects(false)
                .sortPredicates(false)
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.MINIMAL)
                .escapeUnicode(false)
                .trailingDot(true)
                .baseIRI(null)
                .stableBlankNodeIds(false)
                .strictMode(true)
                .validateURIs(true)
                .includeContext(true) // TriG includes contexts by definition
                .useMultilineLiterals(true)
                .lineEnding(SerializationConstants.DEFAULT_LINE_ENDING)
                .build();
    }

    public boolean shouldUseTripleQuotes(String literalValue) {
        return useMultilineLiterals &&
                (literalValue.contains(SerializationConstants.LINE_FEED) || literalValue.contains(SerializationConstants.CARRIAGE_RETURN));
    }

    public boolean shouldOptimizeOutput() {
        return useCompactTriples || groupBySubject || prettyPrint;
    }

    public boolean shouldUseInlineBlankNodes() {
        return blankNodeStyle == BlankNodeStyleEnum.ANONYMOUS && useCompactTriples;
    }
    // --- Getters for all options ---

    public boolean usePrefixes() {
        return usePrefixes;
    }

    public boolean autoDeclarePrefixes() {
        return autoDeclarePrefixes;
    }

    public PrefixOrderingEnum getPrefixOrdering() {
        return prefixOrdering;
    }

    public Map<String, String> getCustomPrefixes() {
        return customPrefixes;
    }

    public boolean useCompactTriples() {
        return useCompactTriples;
    }

    public boolean useRdfTypeShortcut() {
        return useRdfTypeShortcut;
    }

    public boolean useCollections() {
        return useCollections;
    }

    public BlankNodeStyleEnum getBlankNodeStyle() {
        return blankNodeStyle;
    }

    public boolean prettyPrint() {
        return prettyPrint;
    }

    public String getIndent() {
        return indent;
    }

    public int getMaxLineLength() {
        return maxLineLength;
    }

    public boolean groupBySubject() {
        return groupBySubject;
    }

    public boolean sortSubjects() {
        return sortSubjects;
    }

    public boolean sortPredicates() {
        return sortPredicates;
    }

    public LiteralDatatypePolicyEnum getLiteralDatatypePolicy() {
        return literalDatatypePolicy;
    }

    public boolean escapeUnicode() {
        return escapeUnicode;
    }

    public boolean trailingDot() {
        return trailingDot;
    }

    public String getBaseIRI() {
        return baseIRI;
    }

    public boolean stableBlankNodeIds() {
        return stableBlankNodeIds;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public boolean validateURIs() {
        return validateURIs;
    }

    public boolean includeContext() {
        return includeContext;
    }

    public String getLineEnding() {
        return lineEnding;
    }

    public boolean useMultilineLiterals() {
        return useMultilineLiterals;
    }
}
