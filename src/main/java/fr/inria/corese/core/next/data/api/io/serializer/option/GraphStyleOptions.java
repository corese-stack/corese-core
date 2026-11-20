package fr.inria.corese.core.next.data.api.io.serializer.option;

/**
 * Options that control the high-level rendering style of graph-oriented RDF
 * serializers (Turtle, TriG, RDF/XML). They are silently ignored for flat
 * line-based formats (N-Triples, N-Quads).
 */
public interface GraphStyleOptions {

    /**
     * Returns whether prefix declarations should be used to shorten IRIs.
     * When {@code false}, all IRIs are written in full {@code <…>} form.
     */
    boolean usePrefixes();

    /**
     * Returns whether long literal values may be written using triple-quote
     * syntax ({@code """…"""}) instead of escaping newlines.
     */
    boolean useMultilineLiterals();

    /**
     * Returns the preferred style for blank nodes in the output.
     *
     * @see BlankNodeStyle
     */
    BlankNodeStyle getBlankNodeStyle();
}
