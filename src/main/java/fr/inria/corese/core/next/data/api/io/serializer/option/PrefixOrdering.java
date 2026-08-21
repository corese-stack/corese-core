package fr.inria.corese.core.next.data.api.io.serializer.option;

/**
 * Defines the ordering policy for prefix declarations.
 */
public enum PrefixOrdering {
    /**
     * Prefixes are sorted alphabetically by their namespace URI.
     */
    ALPHABETICAL,
    /**
     * Prefixes are declared in the order they are first encountered/used in the graph.
     */
    USAGE_ORDER,
    /**
     * A custom order defined by the user through the customPrefixes map.
     */
    CUSTOM
}