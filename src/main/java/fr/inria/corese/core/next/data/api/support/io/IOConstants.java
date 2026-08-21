package fr.inria.corese.core.next.data.api.support.io;

import fr.inria.corese.core.next.common.config.ConfigurationProperties;
import fr.inria.corese.core.next.data.api.config.DataProperty;

/**
 * Shared constants between serializers and parsers
 */
public final class IOConstants {

    private IOConstants() {

    }

    // Generic characters

    public static final String POINT = ".";

    public static final String SEMICOLON = ";";

    public static final String COMMA = ",";

    public static final String COLON = ":";

    public static final String SPACE = " ";

    public static final String TAB = "\t";

    public static final String LINE_FEED = "\n";

    public static final String HASH = "#";

    public static final String CARRIAGE_RETURN = "\r";

    public static final String SLASH = "/";

    public static final String QUOTE = "\"";

    public static final String SINGLE_QUOTE = "'";

    public static final String AT = "@";

    public static final String LT = "<"; // Less than

    public static final String GT = ">"; // Greater than

    // RDF-specific characters

    /**
     * The blank node prefix.
     */
    public static final String BLANK_NODE_PREFIX = "_:";

    public static final String IRI_START = "<";

    public static final String IRI_END = ">";

    public static final String RDF_TYPE_SHORTCUT = "a";

    public static final String DATATYPE_SEPARATOR = "^^";

    public static final String NEWLINE = LINE_FEED;

    public static final String DEFAULT_LINE_ENDING = LINE_FEED;

    /**
     * Returns the configured default base URI for IRI resolution.
     * The value is configurable via {@code core.default-uri}.
     *
     * @return the configured default base URI
     */
    public static String getDefaultBaseURI() {
        return ConfigurationProperties.instance().getValue(DataProperty.DEFAULT_BASE_URI).orElseThrow();
    }
}
