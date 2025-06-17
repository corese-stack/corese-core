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
     * Represents a backslash character ("\\").
     * Used for escaping special characters within string literals.
     */
    public static final String BACK_SLASH = "\\";

    /**
     * The URI string for the XML Schema `xsd:string` datatype:
     * {@code http://www.w3.org/2001/XMLSchema#string}.
     */
    public static final String XSD_STRING = "http://www.w3.org/2001/XMLSchema#string";

    /**
     * The URI string for the RDF `rdf:langString` datatype:
     * {@code http://www.w3.org/1999/02/22-rdf-syntax-ns#langString}.
     */
    public static final String RDF_LANGSTRING = "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString";

    // Nouvelle constante pour le préfixe de la balise de langue
    public static final String AT_SIGN = "@";

    // Nouvelle constante pour le séparateur de datatype
    public static final String DATATYPE_SEPARATOR = "^^";
    // Used by NAMED BlankNodeStyle
    public static final String DEFAULT_BLANK_NODE_PREFIX = "_:";
    // Separator for predicate-object lists in Turtle/TriG
    public static final String SEMICOLON = ";";
    // Separator for object lists in Turtle/TriG
    public static final String COMMA = ",";
    // 2 spaces
    public static final String DEFAULT_INDENTATION = "  ";

    public static final String DEFAULT_LINE_ENDING = "\n";


    // --- Keywords and Shortcuts ---
    public static final String PREFIX_KEYWORD = "@prefix"; // Turtle/TriG prefix keyword

    public static final String RDF_TYPE_SHORTCUT = "a";  // Shortcut for rdf:type in Turtle/TriG

    public static final String COLON = ":";

    public static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public static final String RDF_TYPE = RDF_NS + "type";
    /**
     * Private constructor to prevent instantiation of this utility class.
     * All members are static.
     */
    private SerializationConstants() {
    }
}