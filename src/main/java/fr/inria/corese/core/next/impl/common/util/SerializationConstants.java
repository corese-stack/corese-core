package fr.inria.corese.core.next.impl.common.util;

/**
 * Provides a collection of constant strings used during the serialization process
 * for various RDF formats. These constants aim to
 * centralize common literal values to ensure consistency and maintainability
 * across different serialization utilities.
 */
public class SerializationConstants {

    /**
     * Represents a single space character (" ").
     * Used for separating elements within an RDF triple or quad.
     */
    public static final String SPACE = " ";

    /**
     * Represents the termination sequence for an RDF statement format:
     * a space, a dot, and a newline character (" .\n").
     */
    public static final String SPACE_POINT = " .\n";

    /**
     * Represents a single dot character (".").
     * Used as a terminator for RDF statements.
     */
    public static final String POINT = ".";

    /**
     * Represents the less-than sign ("&lt;").
     * Used to enclose URIs in N-Triples and N-Quads.
     */
    public static final String LT = "<";

    /**
     * Represents the greater-than sign ("&gt;").
     * Used to enclose URIs.
     */
    public static final String GT = ">";

    /**
     * Represents the standard prefix for blank nodes ("_:").
     * Used to identify blank nodes.
     */
    public static final String BNODE_PREFIX = "_:";

    /**
     * Represents a double-quote character ("\"").
     * Used to enclose literal values.
     */
    public static final String QUOTE = "\"";


    /**
     * Private constructor to prevent instantiation of this utility class.
     * All members are static.
     */
    private SerializationConstants() {}
}