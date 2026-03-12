package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * Built-in functions available in SPARQL 1.0 filter expressions.
 */
public enum BuiltinFunction {

    // --- Accessor functions ---

    /**
     * {@code STR(term)}: returns the string form of a literal or IRI.
     * For a literal, returns its lexical form; for an IRI, returns its IRI string.
     */
    STR,

    /**
     * {@code LANG(literal)}: returns the language tag of a plain literal,
     * or an empty string if the literal has no language tag.
     */
    LANG,

    /**
     * {@code LANGMATCHES(lang, range)}: returns {@code true} if the language tag
     * {@code lang} matches the language range {@code range} per RFC 4647.
     */
    LANGMATCHES,

    /**
     * {@code DATATYPE(literal)}: returns the datatype IRI of a typed literal.
     * For a plain literal with no language tag, returns {@code xsd:string}.
     */
    DATATYPE,

    // --- Existence function ---

    /**
     * {@code BOUND(?var)}: returns {@code true} if the variable {@code ?var}
     * is bound (has a value) in the current solution mapping.
     */
    BOUND,

    // --- Type-testing functions ---

    /**
     * {@code isIRI(term)}: returns {@code true} if {@code term} is an IRI.
     * Equivalent to {@link #IS_URI}.
     */
    IS_IRI,

    /**
     * {@code isURI(term)}: returns {@code true} if {@code term} is a URI.
     * Equivalent to {@link #IS_IRI}.
     */
    IS_URI,

    /**
     * {@code isBlank(term)}: returns {@code true} if {@code term} is a blank node.
     */
    IS_BLANK,

    /**
     * {@code isLiteral(term)}: returns {@code true} if {@code term} is a literal.
     */
    IS_LITERAL
}