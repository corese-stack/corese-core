package fr.inria.corese.core.next.data.api.io.serializer.option;

/**
 * Defines the style for serializing blank nodes in graph-oriented RDF formats (Turtle, TriG).
 */
public enum BlankNodeStyle {
    /**
     * Use the compact {@code []} or {@code [ predicate object ; ... ]} shorthand syntax where possible.
     */
    ANONYMOUS,
    /**
     * Use named blank nodes with generated IDs (e.g., {@code _:b1}, {@code _:b2}).
     */
    NAMED
}
