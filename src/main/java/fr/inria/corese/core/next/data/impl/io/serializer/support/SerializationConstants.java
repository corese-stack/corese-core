package fr.inria.corese.core.next.data.impl.io.serializer.support;

import fr.inria.corese.core.next.data.spi.io.IOConstants;

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

    // Shared RDF I/O syntax, explicitly delegated instead of inherited.
    public static final String POINT = IOConstants.POINT;
    public static final String SEMICOLON = IOConstants.SEMICOLON;
    public static final String COMMA = IOConstants.COMMA;
    public static final String COLON = IOConstants.COLON;
    public static final String SPACE = IOConstants.SPACE;
    public static final String TAB = IOConstants.TAB;
    public static final String LINE_FEED = IOConstants.LINE_FEED;
    public static final String HASH = IOConstants.HASH;
    public static final String CARRIAGE_RETURN = IOConstants.CARRIAGE_RETURN;
    public static final String SLASH = IOConstants.SLASH;
    public static final String QUOTE = IOConstants.QUOTE;
    public static final String AT = IOConstants.AT;
    public static final String LT = IOConstants.LT;
    public static final String GT = IOConstants.GT;
    public static final String BLANK_NODE_PREFIX = IOConstants.BLANK_NODE_PREFIX;
    public static final String IRI_START = IOConstants.IRI_START;
    public static final String IRI_END = IOConstants.IRI_END;
    public static final String RDF_TYPE_SHORTCUT = IOConstants.RDF_TYPE_SHORTCUT;

    // --- Common Delimiters and Special Characters in Serialization ---
    public static final String NEWLINE = LINE_FEED;
    public static final String CARET = "^";
    public static final String BACK_SLASH = "\\";


    // Turtle-specific
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

    // XML-specific constants
    public static final String XML_DECLARATION_START = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    public static final String RDF_ROOT_START = "<rdf:RDF";
    public static final String RDF_ROOT_END = "</rdf:RDF>";
    public static final String RDF_DESCRIPTION_START = "<rdf:Description";
    public static final String RDF_DESCRIPTION_END = "</rdf:Description>";
    public static final String RDF_ABOUT_ATTRIBUTE = "rdf:about";
    public static final String RDF_NODEID_ATTRIBUTE = "rdf:nodeID";
    public static final String RDF_RESOURCE_ATTRIBUTE = "rdf:resource";
    public static final String RDF_DATATYPE_ATTRIBUTE = "rdf:datatype";
    public static final String XMLNS_PREFIX = "xmlns:";
    public static final String XML_LANG_ATTRIBUTE = "xml:lang";

    public static final String AMP_ENTITY = "&amp;";
    public static final String LT_ENTITY = "&lt;";
    public static final String GT_ENTITY = "&gt;";
    public static final String QUOT_ENTITY = "&quot;";
    public static final String APOS_ENTITY = "&apos;";

    public static final String C14N = "c14n";

    public static final String  CANONICAL_BNODE_PLACEHOLDER = "<>";
    public static final String  HEX_FORMAT = "%02x";
    public static final String  CANONICAL_BNODE_PREFIX = "_:b";


    // Hash algorithm
    public static final String SHA_256 = "SHA-256";
    public static final String SHA_384 = "SHA-384";

}
