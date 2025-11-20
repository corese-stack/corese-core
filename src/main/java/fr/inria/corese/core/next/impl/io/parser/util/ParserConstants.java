package fr.inria.corese.core.next.impl.io.parser.util;

import fr.inria.corese.core.next.impl.io.common.IOConstants;
import fr.inria.corese.core.util.Property;

/**
 * A utility class containing constants for characters and keywords
 * used in the TriG parser. Centralizing these values helps in
 * maintaining the code and improves readability.
 */
public final class ParserConstants extends IOConstants {


    public static final String BASE = "@base";

    public static final String PREFIX = "@prefix";

    public static final String DOUBLE_DOT = "..";

    public static final String SPARQL_BASE = "BASE";

    public static final String SPARQL_PREFIX = "PREFIX";

    public static final String GRAPH = "GRAPH";

    public static final String EMPTY_STRING = "";

    public static final String E = "e";

    // --- Delimiters and Punctuation ---

    public static final String DOT = ".";


    public static final String EMPTY_SQUARE_BRACKET = "[]";
    // --- IRI and Literal Delimiters ---

    public static final String APOSTROPHE = "'";

    public static final String TRIPLE_QUOTE = "\"\"\"";

    public static final String TRIPLE_APOSTROPHE = "'''";

    public static final String DOUBLE_SLASH = "//";

    public static final String FILE_PROTOCOL_SIMPLE = "file:/";
    public static final String FILE_PROTOCOL_TRIPLE_SLASH = "file:///";
    public static final String FILE_PROTOCOL = "file://";
    public static final String PNAME_NS_PATTERN = "^[A-Za-z_][A-Za-z0-9_-]*$";

    public static final String MINUS = "-";

    public static final String QUERY_MARK = "?";
    public static final String PLUS = "+";

    // Prevent instantiation of this utility class.
    private ParserConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
