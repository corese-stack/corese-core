package fr.inria.corese.core.next.impl.io.parser.util;

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

    public static final String OPEN_BRACE = "{";

    public static final String CLOSE_BRACE = "}";

    public static final String OPEN_PARENTHESIS = "(";

    public static final String CLOSE_PARENTHESIS = ")";

    public static final String OPEN_SQUARE_BRACKET = "[";

    public static final String CLOSE_SQUARE_BRACKET = "]";

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

    public static final String DEFAULT_BASE_URI = "http://example.org/";

    public static final String RDF_TRG_TEST_SUITE_URI = "https://w3c.github.io/rdf-tests/rdf/rdf11/rdf-trig/";


    public static final String SPACE = " ";
    public static final String TAB = "\t";
    public static final String LINE_FEED = "\n";
    public static final String CARRIAGE_RETURN = "\r";

    // Prevent instantiation of this utility class.
    private ParserConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
