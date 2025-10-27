package fr.inria.corese.core.next.impl.io.parser.util;

import fr.inria.corese.core.util.Property;

/**
 * A utility class containing constants for characters and keywords
 * used in the TriG parser. Centralizing these values helps in
 * maintaining the code and improves readability.
 */
public final class ParserConstants {


    public static final String BASE = "@base";

    public static final String PREFIX = "@prefix";

    public static final String POINT = ".";

    public static final String DOUBLE_DOT = "..";

    public static final String SPARQL_BASE = "BASE";

    public static final String SPARQL_PREFIX = "PREFIX";

    public static final String GRAPH = "GRAPH";

    public static final String A = "a";

    public static final String EMPTY_STRING = "";

    public static final String E = "e";

    // --- Delimiters and Punctuation ---

    public static final String COLON = ":";

    public static final String SEMICOLON = ";";

    public static final String COMMA = ",";

    public static final String DOT = ".";


    public static final String EMPTY_SQUARE_BRACKET = "[]";
    // --- IRI and Literal Delimiters ---

    public static final String IRI_START = "<";

    public static final String IRI_END = ">";

    public static final String QUOTE = "\"";

    public static final String APOSTROPHE = "'";

    public static final String TRIPLE_QUOTE = "\"\"\"";

    public static final String TRIPLE_APOSTROPHE = "'''";

    public static final String SLASH = "/";

    public static final String DOUBLE_SLASH = "//";


    public static final String AT = "@";
    /**
     * The blank node prefix.
     */
    public static final String BLANK_NODE_PREFIX = "_:";

    public static final String FILE_PROTOCOL_SIMPLE = "file:/";
    public static final String FILE_PROTOCOL_TRIPLE_SLASH = "file:///";
    public static final String FILE_PROTOCOL = "file://";
    public static final String RDF_TRG_TEST_SUITE_URI = "https://w3c.github.io/rdf-tests/rdf/rdf11/rdf-trig/";
    public static final String PNAME_NS_PATTERN = "^[A-Za-z_][A-Za-z0-9_-]*$";

    public static final String MINUS = "-";
    public static final String SPACE = " ";
    public static final String TAB = "\t";
    public static final String LINE_FEED = "\n";
    public static final String CARRIAGE_RETURN = "\r";

    public static final String FRAGMENT = "#";
    public static final String QUERY_MARK = "?";
    public static final String PLUS = "+";

    // Prevent instantiation of this utility class.
    private ParserConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    /**
     * Returns the configured default base URI for IRI resolution.
     * The value is configurable via {@code Property.Value.DEFAULT_BASE_URI}.
     *
     * @return the default base URI from configuration, or null if not set
     */
    public static String getDefaultBaseURI() {
        return Property.getStringValue(Property.Value.DEFAULT_BASE_URI);
    }
}
