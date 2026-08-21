package fr.inria.corese.core.next.common.text;

import java.util.Locale;

/** Text helpers for RDF and SPARQL lexical forms. */
public final class RdfText {

    private RdfText() {
    }

    /**
     * Strips enclosing angle brackets {@code <...>} from an IRI string if present.
     *
     * @param value the IRI string to strip
     * @return the IRI string without enclosing angle brackets
     */
    public static String stripAngleBrackets(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("<") && trimmed.endsWith(">")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * Strips a trailing colon {@code :} from a prefix string if present.
     *
     * @param prefix the prefix string to strip
     * @return the prefix string without trailing colon
     */
    public static String stripTrailingColon(String prefix) {
        String trimmed = prefix.trim();
        if (trimmed.endsWith(":")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * Returns a human-readable description of a character for error messages.
     *
     * @param c the character to describe
     * @return human-readable description
     */
    public static String describeCharacter(char c) {
        switch (c) {
            case 0x00:
                return "null character";
            case 0x09:
                return "tab";
            case 0x0A:
                return "line feed";
            case 0x0D:
                return "carriage return";
            case 0x20:
                return "space";
            case 0x7F:
                return "delete";
            case '<':
                return "less than";
            case '>':
                return "greater than";
            case '{':
                return "left curly bracket";
            case '}':
                return "right curly bracket";
            case '\\':
                return "backslash";
            case '^':
                return "circumflex";
            case '`':
                return "grave accent";
            case '|':
                return "pipe";
            case '"':
                return "quotation mark";
            default:
                if (c < 0x20) {
                    return "control character";
                } else if (c >= 0x80 && c <= 0x9F) {
                    return "high control character";
                } else {
                    return String.format("character '%c'", c);
                }
        }
    }

    /**
     * Escapes characters in a string for display in error messages.
     *
     * @param iri the IRI to escape for display
     * @return escaped version suitable for error messages
     */
    public static String escapeForDisplay(String iri) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < iri.length(); i++) {
            char c = iri.charAt(i);
            if (c < 0x20 || (c >= 0x7F && c <= 0x9F)) {
                // Display control characters as Unicode escapes
                sb.append(String.format("\\u%04X", (int) c));
            } else if (c > 0x7E) {
                // Display non-ASCII as Unicode escapes for clarity
                sb.append(String.format("\\u%04X", (int) c));
            } else if (c == '<' || c == '>' || c == '{' || c == '}' || c == '\\' || c == '^' || c == '`' || c == '|' || c == '"') {
                // Display reserved characters with backslash escape
                sb.append('\\').append(c);
            } else {
                // Display normal ASCII characters as-is
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Strips angle brackets if present, then returns the local part after {@code #}, {@code /}, or {@code :},
     * lowercased.
     */
    public static String localNameFromIriToken(String raw) {
        String t = raw.trim();
        if (t.length() >= 2 && t.charAt(0) == '<' && t.charAt(t.length() - 1) == '>') {
            t = t.substring(1, t.length() - 1);
        }
        int hash = t.lastIndexOf('#');
        if (hash >= 0 && hash < t.length() - 1) {
            return t.substring(hash + 1).toLowerCase(Locale.ROOT);
        }
        int slash = t.lastIndexOf('/');
        if (slash >= 0 && slash < t.length() - 1) {
            return t.substring(slash + 1).toLowerCase(Locale.ROOT);
        }
        int colon = t.lastIndexOf(':');
        if (colon >= 0 && colon < t.length() - 1) {
            return t.substring(colon + 1).toLowerCase(Locale.ROOT);
        }
        return t.toLowerCase(Locale.ROOT);
    }

    /**
     * Remove starting "?" or "$" and surrounding whitespaces
     */
    public static String stripVariableMarker(String s) {
        String trimmed = s.trim();
        return trimmed.startsWith("?") || trimmed.startsWith("$") ? trimmed.substring(1) : trimmed;
    }
}
