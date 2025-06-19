package fr.inria.corese.core.next.impl.common.util;

/**
 * Provides common constants used throughout the RDF serialization process.
 * This includes URIs for common RDF, RDFS, XSD, and OWL vocabularies,
 * as well as various special characters and strings used in serialization formats
 * like Turtle, N-Triples, and N-Quads.
 */
public final class SerializationConstants {

    private SerializationConstants() {
        // Private constructor to prevent instantiation
    }

    // --- Standard RDF/RDFS/XSD/OWL URIs ---
    public static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";
    public static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";
    public static final String OWL_NS = "http://www.w3.org/2002/07/owl#";

    public static final String RDF_TYPE = RDF_NS + "type";
    public static final String RDF_FIRST = RDF_NS + "first";
    public static final String RDF_REST = RDF_NS + "rest";
    public static final String RDF_NIL = RDF_NS + "nil";
    public static final String RDF_LANGSTRING = RDF_NS + "langString";

    public static final String XSD_STRING = XSD_NS + "string";
    public static final String XSD_INTEGER = XSD_NS + "integer";
    public static final String XSD_DECIMAL = XSD_NS + "decimal";
    public static final String XSD_DOUBLE = XSD_NS + "double";
    public static final String XSD_BOOLEAN = XSD_NS + "boolean";
    public static final String XSD_DATETIME = XSD_NS + "dateTime";

    // Nouveau namespace FOAF
    public static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";


    // --- Common Delimiters and Special Characters in Serialization ---
    public static final String SPACE = " ";
    public static final String TAB = "\t";
    public static final String LINE_FEED = "\n";
    public static final String CARRIAGE_RETURN = "\r";
    public static final String NEWLINE = LINE_FEED;

    public static final String POINT = ".";
    public static final String SEMICOLON = ";";
    public static final String COMMA = ",";
    public static final String AT_SIGN = "@";
    public static final String CARET = "^";
    public static final String LT = "<"; // Less than
    public static final String GT = ">"; // Greater than
    public static final String QUOTE = "\"";
    public static final String COLON = ":";
    public static final String BACK_SLASH = "\\";

    // Nouveaux délimiteurs
    public static final String HASH = "#";
    public static final String SLASH = "/";


    // Turtle-specific
    public static final String RDF_TYPE_SHORTCUT = "a";
    public static final String BNODE_PREFIX = "_:";
    public static final String DATATYPE_SEPARATOR = "^^";
    public static final String BLANK_NODE_START = "[";
    public static final String BLANK_NODE_END = "]";

    public static final String OPEN_PARENTHESIS = "(";
    public static final String CLOSE_PARENTHESIS = ")";

    // --- Default Values for Configuration ---
    public static final String DEFAULT_INDENTATION = "  "; // Two spaces
    public static final String DEFAULT_LINE_ENDING = "\n"; // Unix-style

    public static final String EMPTY_STRING = "";

    // TriG-specific
    public static final String OPEN_BRACE = "{";
    public static final String CLOSE_BRACE = "}";

}
