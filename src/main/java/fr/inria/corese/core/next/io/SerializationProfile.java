package fr.inria.corese.core.next.io;

import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializerOptions;
import fr.inria.corese.core.next.data.api.io.serializer.option.BlankNodeStyle;
import fr.inria.corese.core.next.data.api.io.serializer.option.LiteralDatatypePolicy;
import fr.inria.corese.core.next.data.api.io.serializer.option.PrefixOrdering;

/**
 * Named serialization profiles providing ready-made option sets for common use cases.
 *
 * <ul>
 *   <li>{@link #COMPACT} — minimal whitespace, optimized for network transfer and IPC.</li>
 *   <li>{@link #PRETTY} — human-friendly indentation and aligned predicate-object blocks.</li>
 *   <li>{@link #CANONICAL} — deterministically sorted output for the same graph terms;
 *       useful for reproducible builds and git diffs, but not a full RDF graph
 *       canonicalization.</li>
 * </ul>
 *
 * <p>Use with {@link CoreseIO}:</p>
 * <pre>{@code
 * String turtle = CoreseIO.writeToString(model, RDFFormat.TURTLE, SerializationProfile.CANONICAL);
 * }</pre>
 */
public enum SerializationProfile {

    /**
     * Minimal whitespace layout.
     * Subjects, predicates and objects are written without extra indentation or blank lines.
     * Optimized for streaming, REST endpoints, and inter-process communication.
     */
    COMPACT,

    /**
     * Human-readable, indented layout.
     * Enables pretty-printing with aligned predicate-object blocks.
     * Best suited for human inspection and debugging.
     */
    PRETTY,

    /**
     * Deterministically sorted output for the same graph terms.
     * Subjects, predicates, and objects are sorted lexicographically.
     * Blank node IDs are assigned in first-appearance order after sorting, and all
     * literal datatypes are written explicitly. This produces repeatable output for
     * the same graph terms, but does not canonicalize blank nodes across isomorphic
     * graphs that use different blank node IDs; use RDFC 1.0 for that purpose.
     */
    CANONICAL;

    /**
     * Returns a configured {@link RDFSerializerOptions} instance for this profile.
     * The options can be passed directly to {@link CoreseIO} or the serializer factory.
     *
     * @return a new {@link RDFSerializerOptions} reflecting this profile's settings
     */
    public RDFSerializerOptions toOptions() {
        return switch (this) {
            case COMPACT  -> buildCompact();
            case PRETTY   -> buildPretty();
            case CANONICAL -> buildCanonical();
        };
    }

    private static RDFSerializerOptions buildCompact() {
        return RDFSerializerOptions.builder()
                .prettyPrint(false)
                .sortSubjects(false)
                .sortPredicates(false)
                .sortObjects(false)
                .stableBlankNodeIds(false)
                .usePrefixes(true)
                .useMultilineLiterals(false)
                .blankNodeStyle(BlankNodeStyle.NAMED)
                .literalDatatypePolicy(LiteralDatatypePolicy.MINIMAL)
                .prefixOrdering(PrefixOrdering.ALPHABETICAL)
                .build();
    }

    private static RDFSerializerOptions buildPretty() {
        return RDFSerializerOptions.builder()
                .prettyPrint(true)
                .indent("  ")
                .maxLineLength(100)
                .sortSubjects(false)
                .sortPredicates(false)
                .sortObjects(false)
                .stableBlankNodeIds(false)
                .usePrefixes(true)
                .useMultilineLiterals(true)
                .blankNodeStyle(BlankNodeStyle.ANONYMOUS)
                .literalDatatypePolicy(LiteralDatatypePolicy.MINIMAL)
                .prefixOrdering(PrefixOrdering.ALPHABETICAL)
                .build();
    }

    private static RDFSerializerOptions buildCanonical() {
        return RDFSerializerOptions.builder()
                .prettyPrint(false)
                .sortSubjects(true)
                .sortPredicates(true)
                .sortObjects(true)
                .stableBlankNodeIds(true)
                .usePrefixes(false)
                .useMultilineLiterals(false)
                .blankNodeStyle(BlankNodeStyle.NAMED)
                .literalDatatypePolicy(LiteralDatatypePolicy.ALWAYS_TYPED)
                .prefixOrdering(PrefixOrdering.ALPHABETICAL)
                .build();
    }
}
