package fr.inria.corese.core.next.query.impl.parser.util;

/**
 * Utility methods for SPARQL syntax normalization.
 *
 * <p>
 * These helpers operate at the parser/AST boundary and remove
 * SPARQL surface syntax artifacts (such as angle brackets around IRIs)
 * before constructing semantic AST nodes.
 * </p>
 */
public final class SyntaxUtils {
    private SyntaxUtils() {
    }

    /**
     * Removes enclosing angle brackets from an IRI if present.
     *
     * <p>
     * Example:
     * {@code "<http://example.org/>"} → {@code "http://example.org/"}
     * </p>
     *
     * @param raw raw IRI string from parser (may include '<' and '>')
     * @return cleaned IRI without angle brackets
     */

    public static String normalizeIri(String raw) {
        if (raw == null) {
            return null;
        }

        if (raw.length() >= 2 && raw.startsWith("<") && raw.endsWith(">")) {
            return raw.substring(1, raw.length() - 1);
        }

        return raw;
    }
}
