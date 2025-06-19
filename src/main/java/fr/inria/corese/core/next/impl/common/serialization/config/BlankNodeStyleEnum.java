package fr.inria.corese.core.next.impl.common.serialization.config;

/**
 * Defines the style for serializing blank nodes.
 */
public enum BlankNodeStyleEnum {
    /**
     * Use the compact '[]' or '[ predicate object ; ... ]' shorthand syntax where possible.
     */
    ANONYMOUS,
    /**
     * Use named blank nodes with generated IDs (e.g., '_:b1', '_:b2').
     */
    NAMED
}