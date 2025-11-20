package fr.inria.corese.core.next.impl.io.common;

import fr.inria.corese.core.util.Property;

/**
 * Shared constants between serializers and parsers
 */
public class IOConstants {

    protected IOConstants() {

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

    /**
     * Returns the configured default base URI for IRI resolution.
     * The value is configurable via {@code Property.Value.DEFAULT_BASE_URI}.
     *
     * @return the default base URI from configuration, or null if not set
     */
    public static String getDefaultBaseURI() {
        String configuredValue = Property.getStringValue(Property.Value.DEFAULT_BASE_URI);
        if (configuredValue == null || configuredValue.isEmpty()) {
            return "http://example.org/";
        }
        return configuredValue;
    }
}
